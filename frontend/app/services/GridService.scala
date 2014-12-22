package services

import configuration.Config

case class GridConfig(url: String, apiUrl: String, key: String)

class GridService {

  //confirm url starts with media service

  def isUrlCorrectFormat(url: String) = {
    println("----")
    println(url)
    println(Config.gridConfig)
    println("----")
    url.startsWith(Config.gridConfig.url)

  }

  //get the equivalent api url

  //store the crop requested

  //call api and return all crops

  //return back to frontend the requested crop

}
