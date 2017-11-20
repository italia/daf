package it.gov.daf.play.filters

import javax.inject.Inject

import play.api.http.DefaultHttpFilters

class FiltersSecurity @Inject()(securityFilter: DafSecurityFilter) extends DefaultHttpFilters(securityFilter)
