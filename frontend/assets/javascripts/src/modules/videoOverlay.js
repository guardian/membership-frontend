/* global YT */
define(['src/modules/raven'],function(raven) {
    'use strict';

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
                playerApi = new YT.Player(playerIframe, {
                    events: {
                        'onReady': function() {
                            playerReady(player, playerApi, playerOverlay);
                        }
                    }
                });
            }
        });
    };

    function playerReady(player, playerApi, playerOverlay) {
        playerOverlay.addEventListener('click', function(event) {
            event.preventDefault();

            if(!iOSDevice()) {
                try {
                    playerApi.playVideo();
                } catch(e) {
                    raven.Raven.captureException(e, {tags: { level: 'info' }});
                }
            }
            player.classList.add(CLASSNAME_IS_PLAYING);
            setTimeout(function() {
                var parentNode = playerOverlay.parentNode;
                if (parentNode) {
                    parentNode.removeChild(playerOverlay);
                }
            }, 2000);
        });
    }

    function init() {
        if (playerEls.length) {
            curl(['js!//www.youtube.com/iframe_api?noext']);
        }
    }

    return {
        init: init
    };

});
