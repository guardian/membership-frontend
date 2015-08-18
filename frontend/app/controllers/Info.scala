package controllers

import play.api.mvc.Controller
import views.support.Asset
import scala.concurrent.Future
import configuration.CopyConfig
import forms.MemberForm._
import model._
import model.Benefits.ComparisonItem
import services.{AuthenticationService, EmailService}

trait Info extends Controller {

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def about = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleAbout,
      request.path,
      Some(CopyConfig.copyDescriptionAbout)
    )
    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("About Membership"),
        availableImages=ResponsiveImageGenerator(
          id="0f30a12f5d00d2b0e445e81c171cd0168d3cf5a7/0_0_1140_684",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("guardian-live"),
        altText=Some("Guardian Live: Experience the Guardian brought to life"),
        availableImages=ResponsiveImageGenerator(
          id="76ef58a05920591099012edb80e7415379392a4c/0_0_1140_684",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("patrons"),
        altText=Some("Become a Patron and support the Guardian's future"),
        availableImages=ResponsiveImageGenerator(
          id="175fba5c61d2b398973befa5d6b49c1257740d5c/0_0_1140_683",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("masterclasses"),
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator(
          id="ae3ad30b485e9651a772e85dd82bae610f57a034/0_0_1140_684",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("midland-goods-shed"),
        altText=Some("A home for big ideas"),
        availableImages=ResponsiveImageGenerator(
          id="ed9347da5fc1e55721b243a958d42fca1983d012/0_0_1140_684",
          sizes=List(1000, 500)
        )
      )
    )
    Ok(views.html.info.about(pageInfo, pageImages))
  }

  def supporter = CachedAction { implicit request =>

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        metadata=Some(Grid.Metadata(
          description = Some("""|People from all walks of life gather in Westminster
            | on 17 June, in London, England, to demand the parliament backs
            | action on climate change""".stripMargin),
          byline = None,
          credit = Some("John Phillips/Getty Images")
        )),
        availableImages=Seq(ResponsiveImage(
          Asset.at("images/temp/supporter-intro.jpg"),
          1000
        ))
      ),
      ResponsiveImageGroup(
        name=Some("fearless"),
        metadata=Some(Grid.Metadata(
          description = Some("Reviewing designs on wall - Supporter"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="8cc033c84791f1583fc52f337d3c6c3ffb368f8e/0_0_1999_1200",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("join"),
        metadata=Some(Grid.Metadata(
          description = Some("Polly Toynbee, Guardian columnist"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="17a31ff294d3c77274091c5e078713fc06ef5cd2/0_0_1999_1200",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.info.supporter(PageInfo(
      CopyConfig.copyTitleSupporters,
      request.path,
      Some(CopyConfig.copyDescriptionSupporters)
    ), pageImages))
  }

  def supporterUSA = CachedAction { implicit request =>

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        metadata=Some(Grid.Metadata(
          description = Some("People take part in a demonstration in Times Square, New York, November 27, 2014."),
          byline = None,
          credit = Some("Carlo Allegri/REUTERS")
        )),
        availableImages=ResponsiveImageGenerator(
          id="8eea3b3bd80eb2f8826b1cef75799d27a11e56e5/293_409_3206_1924",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("fearless"),
        metadata=Some(Grid.Metadata(
          description = Some("The Counted: people killed by police in the United States in 2015"),
          byline = Some("The Guardian US"),
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="201ae0837f996f47b75395046bdbc30aea587443/0_0_1140_684",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("join"),
        metadata=Some(Grid.Metadata(
          description = Some("Katharine Viner, editor-in-chief of the Guardian"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="e2e62954254813fd6781952a56f18bf20343ed0a/0_0_2000_1200",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.info.supporterUSA(PageInfo(
      CopyConfig.copyTitleSupporters,
      request.path,
      Some(CopyConfig.copyDescriptionSupporters)
    ), pageImages))

  }

  def patron() = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitlePatrons,
      request.path,
      Some(CopyConfig.copyDescriptionPatrons)
    )
    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("Patrons of the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("independence"),
        altText=Some("Katharine Viner, editor-in-chief of the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="a4856412e2bef82e6d1d4ce5220fe2391e3f5ca5/0_0_2000_1200",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("backstage-pass"),
        altText=Some("Backstage pass to the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="83afa3867ef76d82c86291f4387b5799c26e07f8/0_0_1140_684",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("get-involved"),
        altText=Some("Choose to get involved"),
        availableImages=ResponsiveImageGenerator(
          id="ed27aaf7623aebc5c8c6d6c8340f247ef7b78ab0/0_0_2000_1200",
          sizes=List(1000,500)
        )
      )
    )
    Ok(views.html.info.patron(pageInfo, pageImages))
  }

  def subscriberOffer = CachedAction { implicit request =>

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("Guardian Live Audience"),
        availableImages=ResponsiveImageGenerator(
          id="38dafd8e470b0d7b3399034f0ccbcce63a0dff25/0_0_1140_684",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("guardian-live"),
        altText=Some("Guardian Live"),
        availableImages=ResponsiveImageGenerator(
          id="9fa0dfc49eb89ec70efe163755564bf0f632fabf/0_0_2279_1368",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.info.subscriberOffer(pageImages))
  }

  def joinChallenger =  CachedAction { implicit request =>

    val pageInfo = PageInfo(
      "Join",
      request.path,
      None,
      hasBackgroundImage = false
    )

    val comparisonItems = Seq(
      ComparisonItem("Priority booking to all Guardian Live and Local events", false, true),
      ComparisonItem("Save 20% on Guardian Live and Local tickets", false, true),
      ComparisonItem("Bring a guest with the same discount and priority booking privileges", false, true),
      ComparisonItem("Save 20% on Guardian Masterclasses", false, true),
      ComparisonItem("Support fearless, open, independent journalism", true, true),
      ComparisonItem("Regular updates from the membership team", true, true),
      ComparisonItem("Exclusive offers and competitions", true, true),
      ComparisonItem("Membership card and annual gift", true, true),
      ComparisonItem("Highlights and live streams of selected Guardian Live events", true, true)
    )

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("experience"),
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator(
          id="eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("support"),
        altText=Some("Support the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("exclusive"),
        altText=Some("Exclusive content"),
        availableImages=ResponsiveImageGenerator(
          id="4bea41f93f7798ada3d572fe07b1e38dacb2a56e/0_0_2000_1200",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("brand-live"),
        altText=Some("Guardian Live"),
        availableImages=ResponsiveImageGenerator(
          id="ed27aaf7623aebc5c8c6d6c8340f247ef7b78ab0/0_0_2000_1200",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("brand-local"),
        altText=Some("Guardian Local"),
        availableImages=ResponsiveImageGenerator(
          id="889926d3c2ececf4ffd699f43713264697823251/0_0_2000_1200",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("brand-masterclasses"),
        altText=Some("Guardian Masterclasses"),
        availableImages=ResponsiveImageGenerator(
          id="ae3ad30b485e9651a772e85dd82bae610f57a034/0_0_1140_684",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("space"),
        altText=Some("A home for big ideas"),
        availableImages=ResponsiveImageGenerator(
          id="ed9347da5fc1e55721b243a958d42fca1983d012/0_0_1140_684",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("patrons"),
        altText=Some("Patrons of The Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="a0b637e4dc13627ead9644f8ec9bd2cc8771f17d/0_0_2000_1200",
          sizes=List(500)
        )
      )
    )

    Ok(views.html.info.joinChallenger(
      pageInfo,
      pageImages,
      comparisonItems
    ))
  }

  def feedback = NoCacheAction { implicit request =>
    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    Ok(views.html.info.feedback(flashMsgOpt))
  }

  def submitFeedback = NoCacheAction.async { implicit request =>

    val userOpt = AuthenticationService.authenticatedUserFor(request)
    val uaOpt = request.headers.get(USER_AGENT)

    def sendFeedback(formData: FeedbackForm) = {
      EmailService.sendFeedback(formData, userOpt, uaOpt)

      Future.successful(Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us"))
    }

    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

}

object Info extends Info
