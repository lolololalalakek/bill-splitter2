package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.keycloak.KeycloakUserRepresentation;

public interface KeycloakAdminService {

    /**
     * Create a user in Keycloak
     * @param user User representation
     * @return Keycloak user ID
     */
    String createUser(KeycloakUserRepresentation user);

    /**
     * Update a user in Keycloak
     * @param userId Keycloak user ID
     * @param user Updated user representation
     */
    void updateUser(String userId, KeycloakUserRepresentation user);

    /**
     * Delete a user from Keycloak
     * @param userId Keycloak user ID
     */
    void deleteUser(String userId);

    /**
     * Enable or disable a user in Keycloak
     * @param userId Keycloak user ID
     * @param enabled Enable/disable flag
     */
    void setUserEnabled(String userId, boolean enabled);

    /**
     * Reset user password in Keycloak
     * @param userId Keycloak user ID
     * @param newPassword New password
     * @param temporary Whether the password is temporary
     */
    void resetPassword(String userId, String newPassword, boolean temporary);

    /**
     * Assign role to user in Keycloak
     * @param userId Keycloak user ID
     * @param roleName Role name to assign
     */
    void assignRealmRole(String userId, String roleName);

    /**
     * Remove role from user in Keycloak
     * @param userId Keycloak user ID
     * @param roleName Role name to remove
     */
    void removeRealmRole(String userId, String roleName);
}
