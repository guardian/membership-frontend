package services

import configuration.Config

case class GridConfig(url: String, apiUrl: String, key: String)

class GridService {

  def isUrlCorrectFormat(url: String) = url.startsWith(Config.gridConfig.url)

  def getApiUrl(url: String) = url.replace(Config.gridConfig.url, Config.gridConfig.apiUrl)

  //store the crop requested

  //call api and return all crops

  //return back to frontend the requested crop

}
