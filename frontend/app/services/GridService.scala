package services

import com.netaporter.uri.Uri
import configuration.Config
import com.netaporter.uri.Uri.parse

case class GridConfig(url: String, apiUrl: String, key: String)

class GridService {

  def isUrlCorrectFormat(url: String) = url.startsWith(Config.gridConfig.url)

  def getApiUrl(url: String) = url.replace(Config.gridConfig.url, Config.gridConfig.apiUrl)

  def getCropRequested(urlString: String) = {
    val uri = parse(urlString)
    uri.query.param("crop")
  }

  //store the crop requested

  //call api and return all crops

  //return back to frontend the requested crop

}
