package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

//TODO

@Entity
data class RoomTimeSeries {

}

@Entity
data class RoomSample(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timeStamp : Long,
    val value : Double,
)

@Database(
    entities = [RoomTimeSeries::class, RoomSample::class],
    version = 1
)
abstract class RemoteWriteDatabase: RoomDatabase() {

    abstract val dao: RoomDao
}

@Dao
interface RoomDao {
    @Query("")//TODO
    fun insertOneTimeSeriesSample(){

    }

    @Query("") //TODO
    fun getNumberOfTimeSeriesSamples(){

    }

    @Query("") //TODO
    fun getTotalNumberOfSamples(){

    }

}

class RemoteWriteSenderDbStorage(getContext: () -> Context) : RemoteWriteSenderStorage(){

    private val roomDb by lazy {
        Room.databaseBuilder(
            getContext(),
            RemoteWriteDatabase::class.java,
            "contacts.db"
        ).build()
    }

    private fun encodeLabels(labelsList: List<TimeSeriesLabel>) : String{
        //TODO
        var result : String = ""
        for (label in labelsList){
            // check if label contains escape character
            if (label.name.contains("-") || label.value.contains("-")){
                throw IllegalArgumentException("Time series labels should not contain \'-\'")
            }else{
                //TODO
            }
        }
        return result
    }

    private fun decodeLabels(labels : String) : List<TimeSeriesLabel> {
        //TODO
    }
    override fun writeScrapedSample(metricsScrape: MetricsScrape) {
        TODO("Not yet implemented")
    }

    override fun getScrapedSamplesCompressedProtobuf(howMany: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun removeNumberOfScrapedSamples(number: Int) {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getLength(): Int {
        TODO("Not yet implemented")
    }

}