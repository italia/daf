package it.gov.daf.play.filters

import javax.inject.Inject
import play.filters.cors.CORSFilter
import play.api.http.DefaultHttpFilters

class FilterSecurityCORS @Inject()(securityFilter: DafSecurityFilter, corsFilter: CORSFilter)
  extends DefaultHttpFilters(securityFilter, corsFilter)
