package it.almawave.linkeddata.kb.repo

class TestingInMemory extends TestingBaseRDFRepository {

  override val mock: RDFRepositoryBase = RDFRepository.memory()

}