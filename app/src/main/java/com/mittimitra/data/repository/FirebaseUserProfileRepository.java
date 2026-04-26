package com.mittimitra.data.repository;

import androidx.annotation.NonNull;

import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.BackendFunctionsClient;
import com.mittimitra.backend.model.AccountModels;
import com.mittimitra.domain.repository.UserProfileRepository;

public class FirebaseUserProfileRepository implements UserProfileRepository {

    private final BackendFunctionsClient functionsClient;

    public FirebaseUserProfileRepository() {
        this.functionsClient = new BackendFunctionsClient();
    }

    @Override
    public void checkDuplicateAccount(@NonNull AccountModels.DuplicateAccountRequest request,
                                      @NonNull BackendCallback<AccountModels.DuplicateAccountData> callback) {
        functionsClient.checkDuplicateAccount(request, callback);
    }

    @Override
    public void linkAccountIdentity(@NonNull AccountModels.LinkIdentityRequest request,
                                    @NonNull BackendCallback<AccountModels.LinkIdentityData> callback) {
        functionsClient.linkAccountIdentity(request, callback);
    }
}
