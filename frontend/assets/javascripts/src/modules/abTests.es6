//global abTests
import * as ophan from 'src/modules/analytics/ophan';

export function init(){
    if (abTests){
            var data = {};
            for (var test of abTests){
                data[test.testSlug] = {
                    'variantName': test.variantSlug,
                    'complete': 'true'
                }
            }
            ophan.ophan.then(function(o){
                o.record({
                abTestRegister:data
            })});

    }
}
