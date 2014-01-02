package chompdb.server

import chompdb.Database
import chompdb.DatabaseVersionShard
import chompdb.store.VersionedStore

import f1lesystem.LocalFileSystem

abstract class NodeProtocol {
  val chomp: Chomp

  // CLIENT-SIDE

  // TODO: Write tests for these?
  def allAvailableShards: Node => Set[DatabaseVersionShard]
  def availableShards: (Node, Database) => Set[DatabaseVersionShard]
  def availableShardsForVersion: (Node, Database, Long) => Set[DatabaseVersionShard]
  def latestVersion: (Node, Database) => Option[Long]
  def serveVersion: (Node, Database, Long) => Unit

  // TODO: Verify that every node has some shards for this version before
  // this method is run, and that shards meet minimum replication factor

  // TODO: Write test for this
  def clusterServeVersion(db: Database, v: Long) { 
    // local Chomp
    serveVersion(db, Some(v))

    // remote Chomps
    chomp
      .nodes
      .keys
      .foreach { n => serveVersion(n, db, v) }
  }

  def latestRemoteVersions(db: Database): Set[Option[Long]] = chomp
    .nodes
    .keys
    .map { n => latestVersion(n, db) }
    .toSet

  def versionShardsPerNode(db: Database, v: Long): Map[Node, Set[DatabaseVersionShard]] = chomp
    .nodes
    .keys
    .map { n => (n, availableShardsForVersion(n, db, v)) }
    .toMap

  // SERVER-SIDE
  def allLocalShards(): Set[DatabaseVersionShard] = chomp
    .databases
    .map { chomp.localDB(_) }
    .flatMap { db => db
      .versions
      .flatMap { v => db.retrieveShards(v) }
    }
    .toSet

  def localShards(db: Database): Set[DatabaseVersionShard] = chomp
    .localDB(db)
    .versions
    .flatMap { v => chomp
      .localDB(db)
      .retrieveShards(v)
    }
    .toSet

  def serveVersion(db: Database, version: Option[Long]) {
    chomp.serveVersion(db, version)
  }

  def shardsBelowRepFactBeforeUpgrade(vspn: Map[Node, Set[DatabaseVersionShard]]): Set[DatabaseVersionShard] = {
    vspn
      .values
      .toList
      .flatten
      .foldLeft(Map[DatabaseVersionShard, Int]() withDefaultValue 0){
        (s, x) => s + (x -> (1 + s(x)))
      }
      .filter(_._2 < chomp.replicationBeforeVersionUpgrade)
      .keys
      .toSet
  }

  /* IN PROGRESS */
  def switchServedVersion(db: Database) {
    chomp
      .localDB(db)
      .mostRecentVersion
      .foreach { latestLocalDatabaseVersion =>
        // If chomp is not serving db, or if chomp is serving a version of db
        // that is not the latestLocalDatabaseVersion
        if (
          if (chomp.servingVersions.contains(db)) {
            chomp.servingVersions(db).exists(_ != latestLocalDatabaseVersion)
          } else false
        ){
          val versionGroups = latestRemoteVersions(db)
            .groupBy {
              case None => "none"
              case v if (v.get < latestLocalDatabaseVersion) => "older"
              case v if (v.get == latestLocalDatabaseVersion) => "equal"
              case _ => "newer"
            } 

          // TODO: Other cases
          if (versionGroups.contains("equal") && versionGroups.size == 1) {
            migrateClusterToKnownVersion(db, latestLocalDatabaseVersion)
          }
        }    
      }

    def migrateClusterToKnownVersion(db: Database, v: Long) {
      val vspn = versionShardsPerNode(db, v)

      if (vspn.filter(_._2.size > 0).keys == chomp.nodes.keys) {
        val shardsBelowMinReplication = shardsBelowRepFactBeforeUpgrade(vspn)

        if (shardsBelowMinReplication.size == 0) {
          // TODO: Test this
          clusterServeVersion(db, v)
        }
      }
    }
  }
}