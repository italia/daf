/**
  * Created by ale on 06/06/17.
  */
import javax.inject.Inject

import it.gov.daf.play.filters.DafSecurityFilter
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
//TODO check if it used!
class Filters @Inject() (corsFilter: CORSFilter, securityFilter :DafSecurityFilter)
  extends DefaultHttpFilters(corsFilter, securityFilter)
