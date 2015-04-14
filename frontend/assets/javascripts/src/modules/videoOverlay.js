/* global YT: true */
/**
 * Play video when clicking on a video overlay image
 */
define(['src/utils/loadJS'], function (loadJS) {

    var SELECTOR_PLAYER = '.js-video';
    var SELECTOR_PLAYER_IFRAME = '.js-video__iframe';
    var SELECTOR_PLAYER_OVERLAY = '.js-video__overlay';
    var CLASSNAME_IS_PLAYING = 'is-playing';

    var playerEls = document.querySelectorAll(SELECTOR_PLAYER);

    /**
     * Nasty UA detection but calling `playVideo` on iOS
     * results in blank player.
     */
    function iOSDevice() {
        return /(iPad|iPhone|iPod)/g.test(navigator.userAgent);
    }

    /**
     * YouTube API is initialised using global callback function
     */
    window.onYouTubeIframeAPIReady = function() {
        [].forEach.call(playerEls, function(player){
            var playerIframe = player.querySelector(SELECTOR_PLAYER_IFRAME);
            var playerOverlay = player.querySelector(SELECTOR_PLAYER_OVERLAY);
            var playerApi;

            if(playerIframe && playerOverlay) {
                playerApi = new YT.Player(playerIframe);
                playerOverlay.addEventListener('click', function(e) {
                    e.preventDefault();

                    if(!iOSDevice()) {
                        playerApi.playVideo();
                    }
                    player.classList.add(CLASSNAME_IS_PLAYING);
                    setTimeout(function() {
                        playerOverlay.parentNode.removeChild(playerOverlay);
                    }, 2000);
                });
            }

        });
    };

    function init() {
        if (playerEls.length) {
            loadJS('//www.youtube.com/iframe_api');
        }
    }

    return {
        init: init
    };

});
