package bootstrap

case class QueryResponse(count: Int, items: Seq[MyItem])

case class MyItem(pk: String,
                  sk: String,
                  attr1: String,
                  attr2: String,
                  attr3: String,
                  attr4: String
                 )
