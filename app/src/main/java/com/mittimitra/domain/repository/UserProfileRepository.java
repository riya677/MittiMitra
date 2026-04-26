package com.mittimitra.domain.repository;

import androidx.annotation.NonNull;

import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AccountModels;

public interface UserProfileRepository {
    void checkDuplicateAccount(@NonNull AccountModels.DuplicateAccountRequest request,
                               @NonNull BackendCallback<AccountModels.DuplicateAccountData> callback);

    void linkAccountIdentity(@NonNull AccountModels.LinkIdentityRequest request,
                             @NonNull BackendCallback<AccountModels.LinkIdentityData> callback);
}
