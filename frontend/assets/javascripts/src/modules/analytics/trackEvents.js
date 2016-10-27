'use strict';

const metrics = {
	purchase: {
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
export function eventData (tier, transactionType) {

	let event = {
		eventCategory: 'Membership Acquisition'
	};

	if (transactionType === 'purchase') {
		event[metrics.purchase[tier]] = 1;
	} else if (transactionType === 'upgrade') {
		event[metrics.upgrade[tier]] = 1;
	}

}
