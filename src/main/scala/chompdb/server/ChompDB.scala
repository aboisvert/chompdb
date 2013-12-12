package chompdb.server

import chompdb.Database
import chompdb.store.ShardedWriter
import f1lesystem.FileSystem
import scala.collection.mutable.SynchronizedSet

class ChompDB(
	val databases: Seq[Database],
	val replicationFactor: Int,
	val replicationFactorBeforeVersionUpgrade: Int,
	val shardIndex: Int,
	val totalShards: Int,
	val executor: ScheduledExecutor,
	val fs: FileSystem,
	val rootDir: FileSystem#Dir
) {
	/* Returns version number, if any, of the latest database version to download from S3. */
	def getNewVersionNumber(database: Database): Option[Long] = {
		database.versionedStore.mostRecentVersion flatMap { latestRemoteVersion => 
			val localVersionDirectories = (rootDir /+ database.name)
					.listDirectories 
				
			if (localVersionDirectories.size == 0) Some(latestRemoteVersion)
			else {
				val latestLocalVersion = localVersionDirectories
					.map( d => d.filename.toLong )
					.max

				if (latestRemoteVersion > latestLocalVersion) Some(latestRemoteVersion)
				else None
			}
		}
	}

	// Copies a database version from S3 to local filesystem. 

	// TODO: numThreads should not be hard set
	// QUESTION: Does the .version file need to be copied over, as well? It is not currently.
	def downloadDatabaseVersion(database: Database, version: Long) = {
		val numThreads = 5

		val remoteDir = database.versionedStore.versionPath(version)
		val localDir = rootDir /+ database.catalog.name /+ database.name /+ version.toString
		localDir.mkdir()

		val shardWriters = (0 until numThreads) map { i => 
			new ShardedWriter {
				val writers = numThreads
				val writerIndex = i
				val shardsTotal = remoteDir
					.listFiles
					.map(_.filename)
					.filter(_.endsWith(".blob"))
					.size

				val baseDir = remoteDir
			}
		}

		copyShards(shardWriters, localDir)

		def copyShards(writers: Seq[ShardedWriter], versionDir: FileSystem#Dir) {
			for (w <- writers) {
				for (baseFile <- w.shardFiles) {
					copy(baseFile.indexFile, versionDir / baseFile.indexFile.filename)
					copy(baseFile.blobFile, versionDir / baseFile.blobFile.filename)
				}
			}
		}

		def copy(from: FileSystem#File, to: FileSystem#File) {
			from.readAsReader { reader =>
				to.write(reader, from.size)
			}
		}
	}

	def versionExists(database: Database, version: Long): Boolean = {
		(rootDir /+ database.catalog.name /+ database.name /+ version.toString).exists
	}

	// def start() {
	// 	executor.schedule(timerTask, period)
	// }
}