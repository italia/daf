import java.net.URI
import java.net.URLEncoder

val a = "daf://ale/test"
val b = URI.create(a);
val test = URLEncoder.encode("Äppelknyckarjazz", "UTF-8")
println(b.toASCIIString)
println(b)
println(b.toString)
b
val ddd = URLEncoder.encode("daf://ale/test")
println(ddd)
