package com.mittimitra.backend.model;

public final class AccountModels {
    private AccountModels() {
    }

    public static class DuplicateAccountRequest {
        public String phone;
        public String email;
        public String currentUid;
    }

    public static class DuplicateAccountData {
        public boolean duplicate;
        public String existingUid;
        public String maskedName;
        public String maskedPhone;
        public String maskedEmail;
        public String reason;
    }

    public static class LinkIdentityRequest {
        public String targetUid;
        public String phone;
        public String email;
    }

    public static class LinkIdentityData {
        public boolean linked;
        public String status;
    }
}
