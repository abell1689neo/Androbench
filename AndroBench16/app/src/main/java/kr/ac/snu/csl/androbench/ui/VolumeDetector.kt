package kr.ac.snu.csl.androbench.ui

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

object VolumeDetector{
    /*automatically detects the storage volumes (getsdexternal path+ getfile system)*/
    fun detect(context: Context):List<VolumeOption>{
        val options=mutableListOf<VolumeOption>() //list of volumeoption struct

        //1. internal direct (/data)
        val internalPath=context.filesDir.absolutePath
        val info=getMountInfo(internalPath)
        options+= VolumeOption(
            label="Internal /data",
            path=internalPath,
            fsType =info.fsType,
            freeKb = getFreeKb(internalPath),
            readOnly=info.readOnly,
        )

        //2. external
//        context.getExternalFilesDir(null)?.absolutePath?.let{extPath->
//            options+= VolumeOption(
//                label="External",
//                path=extPath,
//                fsType =getFileSystemType(extPath),
//                freeKb = getFreeKb(extPath),
//            )
//        }

        //3. SD card
        context.getExternalFilesDirs(null).drop(1).filterNotNull().forEachIndexed { i, dir->
            if(isWritable(dir)) return@forEachIndexed
            val info=getMountInfo(dir.absolutePath)
            options+= VolumeOption(
                label="SD/USB #${i+1}",
                path=dir.absolutePath,
                fsType =info.fsType,
                freeKb = getFreeKb(dir.absolutePath),
                readOnly=info.readOnly,
            )

        }

        //4. read only
        val readOnlyCandidates=listOf("/system", "/vendor", "/product")
        for(path in readOnlyCandidates){
            val dir=File(path)
            if(!dir.exists()||!dir.canRead()) continue
            val info=getMountInfo(path)
            options+= VolumeOption(
                label=path,
                path=path,
                fsType=info.fsType,
                freeKb = getFreeKb(path),
                readOnly=info.readOnly,
            )
        }

        return options
    }

    //parse /proc/mounts and return file system type
//    private fun getFileSystemType(path: String):String{
//        return try{
//            val mounts=File("/proc/mounts").readLines()
//            //longest prefix matching?
//            mounts
//                .mapNotNull { line->
//                val parts=line.split(" ")
//                if(parts.size>=3) parts else null
//                }
//                .filter { path.startsWith(it[1]) }
//                .maxByOrNull { it[1].length }
//                ?.get(2)
//                ?:"unknown"
//        } catch (e: Exception){
//            "unknown"
//        }
//    }

    private data class MountInfo(val fsType: String, val readOnly: Boolean)
    private fun getMountInfo(path:String):MountInfo{
        return try{
            File("/proc/mounts").readLines()
                .mapNotNull { line->
                    val parts=line.split(" ")
                    if(parts.size>=4) parts else null
                }
                .filter{
                    val mp=it[1]
                    path==mp ||path.startsWith("$mp/")||mp=="/"
                }
                .maxByOrNull { it[1].length }
                ?.let{ parts->
                    val opts=parts[3].split(",")
                    MountInfo(
                        fsType = parts[2],
                        readOnly ="ro" in opts,
                    )
                }?:MountInfo("unknown", false)

        }catch(e:Exception){
            MountInfo("unknown", false)
        }
    }

    private fun getFreeKb(path: String): Long=try{
        val sf= StatFs(path)
        sf.availableBlocksLong*sf.blockSizeLong/1024
    } catch(e: Exception){0}

    private fun isWritable(dir:File): Boolean=try{
        if(!dir.exists()) dir.mkdirs()
        val testFile=File(dir, ".write_test")
        testFile.createNewFile().also{testFile.delete()}
    } catch (e: Exception){false}
}