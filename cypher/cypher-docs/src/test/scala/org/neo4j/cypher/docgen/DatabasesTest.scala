package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class DatabasesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Databases", "administration-databases")
    database("system")
    initQueries(
      "CREATE DATABASE `movies`",
      "CREATE DATABASE `northwind-graph`"
    )
    synopsis("This section explains how to use Cypher to manage Neo4j databases: creating, deleting, starting and stopping individual databases within a single server.")
    p(
      """
        |* <<administration-databases-introduction, Introduction>>
        |* <<administration-databases-show-databases, Listing databases>>
        |* <<administration-databases-create-database, Creating databases>>
        |* <<administration-databases-stop-database, Stopping databases>>
        |* <<administration-databases-start-database, Starting databases>>
        |* <<administration-databases-drop-database, Deleting databases>>
        |""".stripMargin)
    section("Introduction", "administration-databases-introduction") {
      p(
        """Neo4j supports the management of multiple databases within the same DBMS.
          |The metadata for these databases, including the associated security model, is maintained in a special database called the `system` database.
          |All multi-database administrative commands must be run against the `system` database.
          |These administrative commands are automatically routed to the `system` database when connected to the DBMS over Bolt.""".stripMargin)
    }
    section("Listing databases", "administration-databases-show-databases") {
      p("There are three different commands for listing databases. Listing all databases, listing a particular database or listing the default database.")
      p("include::show-databases-syntax.asciidoc[]")
      p("All available databases can be seen using the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("A particular database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE system", assertDatabaseShown("system")) {
        resultTable()
      }
      p("The default database can be seen using the command `SHOW DEFAULT DATABASE`.")
      query("SHOW DEFAULT DATABASE", assertDatabaseShown("neo4j")) {
        resultTable()
      }
      p("It is also possible to filter and sort the results by using `YIELD`, `ORDER BY` and `WHERE`.")
      query("SHOW DATABASES YIELD name, currentStatus, requestedStatus ORDER BY currentStatus WHERE name CONTAINS 'e'",
        assertDatabaseShown("neo4j", "system", "movies")) {
        p(
          """In this example:
            |
            |* The number of columns returned has been reduced with the `YIELD` clause.
            |* The order of the returned columns has been changed.
            |* The results have been filtered to only show database names containing 'e'.
            |* The results are ordered by the 'currentStatus' column using `ORDER BY`.
            |
            |It is also possible to use `SKIP` and `LIMIT` to paginate the results.
            |""".stripMargin)
        resultTable()
      }
      note {
        p(
          """Note that for failed databases, the `currentStatus` and `requestedStatus` are different.
            |This often implies an error, but **does not always**.
            |For example, a database may take a while to transition from `offline` to `online` due to performing recovery.
            |Or, during normal operation a database's `currentStatus` may be transiently different from its `requestedStatus` due to a necessary automatic process, such as one Neo4j instance copying store files from another.
            |The possible statuses are `initial`, `online`, `offline`, `store copying` and `unknown`.
            |""".stripMargin)
      }
    }
    section("Creating databases", "administration-databases-create-database", "enterprise-edition") {
      p("Databases can be created using `CREATE DATABASE`.")
      query("CREATE DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a database has been created, it will show up in the listing provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("This command is optionally idempotent, with the default behavior to throw an exception if the database already exists. " +
        "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the database already exist. " +
        "Adding `OR REPLACE` to the command will result in any existing database being deleted and a new one created.")
      query("CREATE DATABASE customers IF NOT EXISTS", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
      query("CREATE OR REPLACE DATABASE customers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        p("This is equivalent to running `DROP DATABASE customers IF EXISTS` followed by `CREATE DATABASE customers`.")
      }
      note {
        p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
      }
    }
    section("Stopping databases", "administration-databases-stop-database", "enterprise-edition") {
      p("Databases can be stopped using the command `STOP DATABASE`.")
      query("STOP DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("The status of the stopped database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE customers", assertDatabaseShown("customers")) {
        resultTable()
      }
    }
    section("Starting databases", "administration-databases-start-database", "enterprise-edition") {
      p("Databases can be started using the command `START DATABASE`.")
      query("START DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("The status of the started database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE customers", assertDatabaseShown("customers")) {
        resultTable()
      }
    }
    section("Deleting databases", "administration-databases-drop-database", "enterprise-edition") {
      p("Databases can be deleted using the command `DROP DATABASE`.")
      query("DROP DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a database has been deleted, it will no longer show up in the listing provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("This command is optionally idempotent, with the default behavior to throw an exception if the database does not exists. " +
        "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the database not exist.")
      query("DROP DATABASE customers IF EXISTS", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
      p("The `DROP DATABASE` command will remove a database entirely. " +
        "However, you can request that a dump of the store files is produced first, and stored in the path configured using the `dbms.directories.dumps.root` setting (by default `<neo4j-home>/data/dumps`). " +
        "This can be achieved by appending `DUMP DATA` to the command (or `DESTROY DATA` to explicitly request the default behaviour). " +
        "These dumps are equivalent to those produced by `neo4j-admin dump` and can be similarly restored using `neo4j-admin load`.")
      query("DROP DATABASE customers DUMP DATA", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
      p("The options `IF EXISTS` and  `DUMP DATA`/ `DESTROY DATA` can also be combined. An example could look like this:")
      query("DROP DATABASE customers IF EXISTS DUMP DATA", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}

    }
  }.build()

  private def assertDatabasesShown = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
    try {
      val dbNodes = tx.findNodes(Label.label("Database")).asScala.toList
      val dbNames = dbNodes.map(n => n.getProperty("name")).toSet
      val result = p.columnAs[String]("name").toSet
      result should equal(dbNames)
    } finally {
      tx.close()
    }
  })

  private def assertDatabaseShown(expected: String*) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
    try {
      val result = p.columnAs[String]("name").toSet
      result should equal(expected.toSet)
    } finally {
      tx.close()
    }
  })
}
