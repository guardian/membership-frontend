import welcome from "src/modules/welcome";
import slideshow from "src/modules/slideshow";
import toggle from "src/modules/toggle";
import sticky from "src/modules/sticky";
import sectionNav from "src/modules/sectionNav";
import videoOverlay from "src/modules/videoOverlay";
import modal from "src/modules/modal";
import cta from "src/modules/events/cta";
import remainingTickets from "src/modules/events/remainingTickets";
import eventPriceEnhance from "src/modules/events/eventPriceEnhance";
import filterFacets from "src/modules/filterFacets";
import filterLiveSearch from "src/modules/filterLiveSearch";
import * as comparisonTable from "src/modules/comparisonTable";
import patterns from "src/modules/patterns";
import * as paidToPaid from "src/modules/paidToPaid";
import common from "src/common";

// Global
common.init();
welcome.init();
slideshow.init();
toggle.init();
sticky.init();
sectionNav.init();
videoOverlay.init();
modal.init();
comparisonTable.init();

// Events
cta.init();
remainingTickets.init();
eventPriceEnhance.init();

// Filtering
filterFacets.init();
filterLiveSearch.init();

// Pattern library
patterns.init();

paidToPaid.init();


