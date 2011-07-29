package com.excilys.ebi.gatling.statistics.writer

import java.io.File
import java.io.FileWriter

class SeparatedValueFileWriter(val runOn: String, val fileName: String, val separator: String) {
  def writeToFile(values: List[List[String]]) = {
    val dir = new File("results/" + runOn + "/rawdata")
    dir.mkdir
    val file = new File(dir, fileName)
    val fw = new FileWriter(file, true)
    for (value <- values) {
      fw.write(value.mkString("", separator, "\n"))
    }
    fw.close
  }
}