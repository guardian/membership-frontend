'use strict';

const metrics = {
	join: {
		friend: 'metric3',
		supporter: 'metric4',
		partner: 'metric5',
		patron: 'metric6'
	},
	upgrade: {
		supporter: 'metric7',
		partner: 'metric8',
		patron: 'metric9'
	}
};

// Builds the data for the analytics event.
export function eventData (tier) {

	let event = {
		eventCategory: 'Membership Acquisition'
	};

	if (guardian.productData.upgrade) {
		event[metrics.upgrade[tier]] = 1;
	} else {
		event[metrics.join[tier]] = 1;
	}

}
