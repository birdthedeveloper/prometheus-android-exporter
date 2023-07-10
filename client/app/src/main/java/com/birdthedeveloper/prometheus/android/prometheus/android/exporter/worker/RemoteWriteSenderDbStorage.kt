package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

//import android.content.Context
//import androidx.annotation.EmptySuper
//import androidx.room.Dao
//import androidx.room.Database
//import androidx.room.Embedded
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//import androidx.room.Query
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import remote.write.RemoteWrite.TimeSeries
//
///// Room is a relational database
/////     Contains the following tables:
/////     - TimeSeries table:
/////         + labels : List<TimeSeriesLabel> sorted alphabetically and encoded in json
/////     - Sample table:
/////         + id
/////         + timestamp
/////         + value
/////         + TimeSeries foreign key
//
//@Entity
//data class RoomTimeSeries (
//    @PrimaryKey(autoGenerate = false)
//    val labels : String
//)
//
//@Entity
//data class RoomSample(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 0,
//    val timeStamp : Long,
//    val value : Double,
//)
//
//@Serializable
//data class TimeSeriesLabelList(
//    val labels: List<TimeSeriesLabel>
//)
//
//data class TimeSeriesWithSamples(
//    @Embedded val timeSeries: RoomTimeSeries,
//    @Embedded val sample : RoomSample,
//)
//
//@Database(
//    entities = [RoomTimeSeries::class, RoomSample::class],
//    version = 1
//)
//abstract class RemoteWriteDatabase: RoomDatabase() {
//    abstract val dao: RoomDao
//}
//
//@Dao
//interface RoomDao {
//
//    fun insertOneTimeSeriesSample(){
//
//    }
//    @Query("")//TODO
//    private fun insertTimeSeries(){
//
//    }
//
//    @Query("")///TODO
//    private fun insertSamples(){
//
//    }
//
//    //@Query("SELECT * ") //TODO
//    fun getNumberOfTimeSeriesSamples(number : Int) : List<TimeSeriesWithSamples>
//
//    @Query("") //TODO
//    fun getTotalNumberOfSamples(){
//
//    }
//
//}
//
//class RemoteWriteSenderDbStorage(getContext: () -> Context) : RemoteWriteSenderStorage(){
//    companion object{
//        const val dbName = "prometheus.db"
//    }
//
//    private val roomDb by lazy {
//        Room.databaseBuilder(
//            getContext(),
//            RemoteWriteDatabase::class.java,
//            dbName,
//        ).build()
//    }
//
//    private fun encodeLabels(labelsList: List<TimeSeriesLabel>) : String{
//        /// preserve the same order
//        val sorted : List<TimeSeriesLabel> = labelsList.sortedBy { it.name }
//        val timeSeriesLabelList = TimeSeriesLabelList(labels = sorted)
//        return Json.encodeToString(TimeSeriesLabelList.serializer(), timeSeriesLabelList)
//    }
//
//    private fun decodeLabels(labels : String) : List<TimeSeriesLabel> {
//        return Json.decodeFromString<TimeSeriesLabelList>(labels).labels
//    }
//    override fun writeScrapedSample(metricsScrape: MetricsScrape) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getScrapedSamplesCompressedProtobuf(howMany: Int): ByteArray {
//        TODO("Not yet implemented")
//    }
//
//    override fun removeNumberOfScrapedSamples(number: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun isEmpty(): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun getLength(): Int {
//        TODO("Not yet implemented")
//    }
//}