package org.apereo.cas.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>Implementation of {@code ServiceRegistryDao} that uses a MongoDb repository as the backend
 * persistence mechanism. The repository is configured by the Spring application context. </p>
 * <p>The class will automatically create a default collection to use with services. The name
 * of the collection may be specified.
 * It also presents the ability to drop an existing collection and start afresh.
 *
 * @author Misagh Moayyed
 * @since 4.1
 */
public class MongoServiceRegistryDao implements ServiceRegistryDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoServiceRegistryDao.class);

    private String collectionName;

    private boolean dropCollection;

    private MongoOperations mongoTemplate;

    /**
     * Ctor.
     *
     * @param mongoTemplate  mongoTemplate
     * @param collectionName collectionName
     * @param dropCollection dropCollection
     */
    public MongoServiceRegistryDao(final MongoOperations mongoTemplate, final String collectionName, final boolean dropCollection) {
        this.mongoTemplate = mongoTemplate;
        this.collectionName = collectionName;
        this.dropCollection = dropCollection;

        Assert.notNull(this.mongoTemplate);

        if (this.dropCollection) {
            LOGGER.debug("Dropping database collection: [{}]", this.collectionName);
            this.mongoTemplate.dropCollection(this.collectionName);
        }

        if (!this.mongoTemplate.collectionExists(this.collectionName)) {
            LOGGER.debug("Creating database collection: [{}]", this.collectionName);
            this.mongoTemplate.createCollection(this.collectionName);
        }
    }

    public MongoServiceRegistryDao() {
    }
    
    @Override
    public boolean delete(final RegisteredService svc) {
        if (this.findServiceById(svc.getId()) != null) {
            this.mongoTemplate.remove(svc, this.collectionName);
            LOGGER.debug("Removed registered service: [{}]", svc);
            return true;
        }
        return false;
    }

    @Override
    public RegisteredService findServiceById(final long svcId) {
        return this.mongoTemplate.findOne(new Query(Criteria.where("id").is(svcId)),
                RegisteredService.class, this.collectionName);
    }

    @Override
    public RegisteredService findServiceById(final String id) {
        final Pattern pattern = Pattern.compile(id, Pattern.CASE_INSENSITIVE);
        return this.mongoTemplate.findOne(new Query(Criteria.where("serviceId").regex(pattern)),
                RegisteredService.class, this.collectionName);
    }

    @Override
    public List<RegisteredService> load() {
        return this.mongoTemplate.findAll(RegisteredService.class, this.collectionName);
    }

    @Override
    public RegisteredService save(final RegisteredService svc) {
        if (svc.getId() == AbstractRegisteredService.INITIAL_IDENTIFIER_VALUE) {
            ((AbstractRegisteredService) svc).setId(svc.hashCode());
        }
        this.mongoTemplate.save(svc, this.collectionName);
        LOGGER.debug("Saved registered service: [{}]", svc);
        return this.findServiceById(svc.getId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public long size() {
        return this.mongoTemplate.count(new Query(), RegisteredService.class, this.collectionName);
    }
}
