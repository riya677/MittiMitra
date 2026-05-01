package com.mittimitra;

import android.content.Context;
import android.provider.Settings;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

/**
 * Resolves a stable user identifier across Firebase-authenticated and local guest flows.
 */
public final class UserIdentityResolver {

    private static final String LOCAL_GUEST_PREFIX = "guest_local_";

    private UserIdentityResolver() {
    }

    public static String getActiveUserId(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
            return user.getUid();
        }
        SessionManager sessionManager = new SessionManager(context);
        String sessionUserId = sessionManager.getUserId();
        if (sessionUserId == null || sessionUserId.trim().isEmpty()) {
            return null;
        }
        return sessionUserId;
    }

    public static String getActiveUserIdOrCreateGuest(Context context) {
        String userId = getActiveUserId(context);
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        return createOrRestoreLocalGuestIdentity(context).userId;
    }

    public static LocalGuestIdentity createOrRestoreLocalGuestIdentity(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String existingUserId = sessionManager.getUserId();
        String existingName = sessionManager.getUserName();

        if (sessionManager.isGuest()
                && existingUserId != null
                && !existingUserId.trim().isEmpty()) {
            return new LocalGuestIdentity(existingUserId, existingName, extractIpFromName(existingName));
        }

        String localIp = resolveLocalIpv4Address();
        String userId = buildLocalGuestUserId(context, localIp);
        String displayName = (localIp != null && !localIp.trim().isEmpty())
                ? "Guest " + localIp
                : "Guest Farmer";
        sessionManager.saveUser(userId, displayName, true);
        return new LocalGuestIdentity(userId, displayName, localIp);
    }

    private static String buildLocalGuestUserId(Context context, String localIp) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        if (androidId == null || androidId.trim().isEmpty()) {
            androidId = UUID.randomUUID().toString();
        }
        String source = androidId + "|" + (localIp != null ? localIp : "offline");
        String hash = sha256(source);
        return LOCAL_GUEST_PREFIX + hash.substring(0, 20);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static String resolveLocalIpv4Address() {
        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // Best-effort only.
        }
        return null;
    }

    private static String extractIpFromName(String name) {
        if (name == null) return null;
        if (!name.startsWith("Guest ")) return null;
        String[] parts = name.split(" ");
        if (parts.length < 2) return null;
        return parts[1];
    }

    public static final class LocalGuestIdentity {
        public final String userId;
        public final String displayName;
        public final String ipAddress;

        LocalGuestIdentity(String userId, String displayName, String ipAddress) {
            this.userId = userId;
            this.displayName = displayName;
            this.ipAddress = ipAddress;
        }
    }
}
