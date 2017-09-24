/**
  * Created by ale on 19/06/17.
  */

import javax.inject.Inject

//import it.gov.daf.common.filters.authentication.SecurityFilter
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
/*
class Filters @Inject() (corsFilter: CORSFilter, securityFilter :SecurityFilter)
  extends DefaultHttpFilters(corsFilter, securityFilter)
*/
class Filters @Inject() (corsFilter: CORSFilter)
  extends DefaultHttpFilters(corsFilter)