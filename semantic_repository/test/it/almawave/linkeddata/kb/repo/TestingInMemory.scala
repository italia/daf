package it.almawave.linkeddata.kb.repo

import it.almawave.linkeddata.kb.repo.RDFRepository;
import it.almawave.linkeddata.kb.repo.RDFRepositoryBase

class TestingInMemory extends TestingBaseRDFRepository {

  override val mock: RDFRepositoryBase = RDFRepository.memory()

}