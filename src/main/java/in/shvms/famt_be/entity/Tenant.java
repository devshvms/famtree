package in.shvms.famt_be.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Enhanced Tenant entity with configuration and limits
 */
@Data
@Document(collection = "tenants")
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    @Indexed(unique = true)
    private String subdomain; // Optional: tenant1.famtree.com
    
    private TenantStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Subscription and limits
    private SubscriptionTier subscriptionTier;
    private LocalDateTime subscriptionExpiresAt;
    
    // Usage limits
    private TenantLimits limits;
    
    // Current usage statistics
    private TenantUsage currentUsage;
    
    // Tenant-specific settings
    private TenantSettings settings;
    
    // Contact information
    private String primaryContactEmail;
    private String primaryContactPhone;
    
    // Billing information (if needed)
    private String billingEmail;
    
    public enum TenantStatus {
        ACTIVE,
        SUSPENDED,
        TRIAL,
        EXPIRED
    }
    
    public enum SubscriptionTier {
        FREE(100, 5, 1000),           // 100 people, 5 users, 1000 MB storage
        BASIC(500, 10, 5000),          // 500 people, 10 users, 5000 MB storage
        PREMIUM(2000, 50, 20000),      // 2000 people, 50 users, 20 GB storage
        UNLIMITED(-1, -1, -1);         // No limits
        
        private final int maxPeople;
        private final int maxUsers;
        private final int maxStorageMB;
        
        SubscriptionTier(int maxPeople, int maxUsers, int maxStorageMB) {
            this.maxPeople = maxPeople;
            this.maxUsers = maxUsers;
            this.maxStorageMB = maxStorageMB;
        }
        
        public int getMaxPeople() { return maxPeople; }
        public int getMaxUsers() { return maxUsers; }
        public int getMaxStorageMB() { return maxStorageMB; }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantLimits {
        private int maxPeople;
        private int maxUsers;
        private int maxLineages;
        private int maxStorageMB;
        private int maxExportsPerMonth;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantUsage {
        private int currentPeopleCount;
        private int currentUserCount;
        private int currentLineageCount;
        private int currentStorageMB;
        private int exportsThisMonth;
        private LocalDateTime lastExportDate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantSettings {
        // Privacy settings
        private boolean allowGuestViewing;
        private boolean requireEmailVerification;
        
        // Data export settings
        private boolean allowCSVExport;
        private boolean allowJSONExport;
        private boolean allowGEDCOMExport;
        
        // Notification settings
        private boolean emailNotificationsEnabled;
        private boolean auditNotificationsEnabled;
        
        // Display settings
        private String dateFormat; // "YYYY-MM-DD", "DD/MM/YYYY", etc.
        private String defaultLanguage;
        
        // Advanced features
        private boolean enableMediaUpload;
        private boolean enableDocumentAttachment;
        
        // Custom fields (for extensibility)
        private Map<String, Object> customSettings;
    }
    
    /**
     * Check if tenant can add more people
     */
    public boolean canAddPerson() {
        if (limits.getMaxPeople() == -1) return true; // Unlimited
        return currentUsage.getCurrentPeopleCount() < limits.getMaxPeople();
    }
    
    /**
     * Check if tenant can add more users
     */
    public boolean canAddUser() {
        if (limits.getMaxUsers() == -1) return true; // Unlimited
        return currentUsage.getCurrentUserCount() < limits.getMaxUsers();
    }
    
    /**
     * Check if tenant can export data
     */
    public boolean canExport() {
        if (limits.getMaxExportsPerMonth() == -1) return true; // Unlimited
        return currentUsage.getExportsThisMonth() < limits.getMaxExportsPerMonth();
    }
    
    /**
     * Check if subscription is active
     */
    public boolean isSubscriptionActive() {
        if (status != TenantStatus.ACTIVE) return false;
        if (subscriptionExpiresAt == null) return true; // No expiry
        return LocalDateTime.now().isBefore(subscriptionExpiresAt);
    }
}