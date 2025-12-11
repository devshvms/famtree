package in.shvms.famt_be.service;

import in.shvms.famt_be.entity.Tenant;
import in.shvms.famt_be.repositories.mongo.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant createTenant(String name) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        return tenantRepository.save(tenant);
    }
}
