package uk.gov.justice.services.example.cakeshop.persistence;

import uk.gov.justice.services.example.cakeshop.persistence.entity.CakeOrder;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository(forEntity = CakeOrder.class)
public interface CakeOrderRepository extends EntityRepository<CakeOrder, UUID> {


}
