package io.github.scottweaver.zio.aspect

import zio._
import zio.test.TestAspect.{before, beforeAll}
import io.github.scottweaver.models.JdbcInfo
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

object DbMigrationAspect {

  type ConfigurationCallback = (FluentConfiguration) => FluentConfiguration

  private def doMigrate(jdbcInfo: JdbcInfo, configureCallback: ConfigurationCallback, locations: String*) =
    ZIO.attempt {
      val flyway = configureCallback({
        val flyway = Flyway
          .configure()
          .dataSource(jdbcInfo.jdbcUrl, jdbcInfo.username, jdbcInfo.password)

        if (locations.nonEmpty)
          flyway.locations(locations: _*)
        else
          flyway
      })
        .load()
      flyway.migrate
    }

  def migrate(mirgationLocations: String*)(configureCallback: ConfigurationCallback = identity) =
    before(
      ZIO
        .service[JdbcInfo]
        .flatMap(jdbcInfo => doMigrate(jdbcInfo, configureCallback, mirgationLocations: _*))
        .orDie
    )

  def migrateOnce(
    migrationLocations: String*
  )(configureCallback: ConfigurationCallback = identity) =
    beforeAll(
      ZIO
        .service[JdbcInfo]
        .flatMap(jdbcInfo => doMigrate(jdbcInfo, configureCallback, migrationLocations: _*))
        .orDie
    )

}
