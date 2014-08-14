/******************************************************************************/
/* Site-Specific Configuration Variables                                      */
/******************************************************************************/
/* Report Suite ID                                                            */
/* Adobe Analytics is split into report suites.  s_account contains the ID of */
/* the one that should be used for the implmenetation in question             */
/*var s_account="guardiangudev-code"                                          */
/******************************************************************************/
/* Cookie Domain Periods                                                      */
/* The location of the cookies affects the visitor identification and some    */
/* data populated by the s_code.  s_cookieDomainPeriods affects the visitor   */
/* identity.  If the web site is in the guardian.com domain, this should be   */
/* '2'.  If it is within the guardian.co.uk domain it should be '3'.  If it   */
/* is elsewhere, then the number corresponds to the domain of the site.       */
/*                                                                            */
/* s_fpCookieDomainPeriods affects the data created by the s_code.  As the    */
/* implementations may differ between Labs sites and main Guardian sites,     */
/* this value to chosen to ensure that the cookies used by the s_code differ  */
/* from the main sites.  An example of the impact of this is that that the    */
/* number of days since the customer's last visit will be tracked seperately  */
/* for main Guardian sites and Labs sites.  If the site is within the         */
/* guardian.com domain, then use '3'.  If it is within guardian.co.uk, use    */
/* '4'. If the site is not within these domains, the parameter can be ommited */
/* as it will default to the same value as s_cookieDomainPeriods.             */
/*var s_cookieDomainPeriods = "2";                                            */
/*var s_fpCookieDomainPeriods = "3";                                          */
/******************************************************************************/
/* s_code Version                                                             */
/* The s_code version is tracked in Adobe Analytics for audit purposes.  This */
/* should contain the web site name, s_code version and date.                 */
/******************************************************************************/

if (typeof(s)=="undefined") s = new AppMeasurement();
s.account="guardiangu-network"
s.debugTracking=false
s.charSet = "UTF-8";
s.cookieDomainPeriods="2";

/* Link Tracking Config */
s.trackDownloadLinks=true;
s.trackExternalLinks=true;
s.trackInlineStats=true;
s.linkDownloadFileTypes="exe,zip,wav,mp3,mov,mpg,avi,wmv,pdf,doc,docx,xls,xlsx,ppt,pptx";
s.linkInternalFilters="javascript:,adinfo-guardian.co.uk,dating.guardian.co.uk,guardian.co.uk,guardian.greatgetaways.co.uk,guardian.lcplc-online.co.uk,guardian.oddschecker.com,guardian.pickthescore.co.uk,guardian.sportinglife.com,guardian.touch-line.com,guardian.unbiased.co.uk,guardianapis.com,guardianapps.co.uk,guardianbooks.co.uk,guardianbookshop.co.uk,guardiancottages.co.uk,guardiandigitalcomparison.co.uk,guardiandirectsubs.co.uk,guardianeatright.co.uk,guardianecostore.co.uk,guardianenergycomparison.co.uk,guardianenergycomparison.com,guardianfashionstore.co.uk,guardiangardencentre.co.uk,guardiangiftexperiences.co.uk,guardianholidayoffers.co.uk,guardianhomeexchange.co.uk,guardianhomeexchange.com,guardianinvesting.co.uk,guardianjobs.co.uk,guardianjobs.com,guardianjobs.mobi,guardianjobsrecruiter.co.uk,guardiannews.com,guardian-newspaper.com,guardianoffers.co.uk,guardianprofessional.co.uk,guardianpublic.co.uk,guardiansubscriptions.co.uk,guardiantickets.co.uk,guardianvouchercodes.co.uk,guardianweekly.co.uk,guardianweekly.com,id.guardian.co.uk,ivebeenthere.co.uk,jobs.guardian.co.uk,kable.co.uk,money-deals.co.uk,mps-expenses.guardian.co.uk,ogenterprises.co.uk,ogtravelinsurance.co.uk,sixsongsof.me,sixwordmemoirs.co.uk,smarthealthcare.com,sofacinema.co.uk,static.guim.co.uk,theguardian.co.uk,theguardian.com,traffic.outbrain.com,tvlistings.guardian.co.uk";
s.linkLeaveQueryString=false;
s.linkTrackVars="eVar7,prop62,prop61";
s.linkTrackEvents="None";
s.useForcedLinkTracking = false; // Fix an issue with Chrome 22 and under with some very specific configuration

s._tpDST = {
    2012:'3/25,10/28',
    2013:'3/31,10/27',
    2014:'3/30,10/26',
    2015:'3/29,10/25',
    2016:'3/27,10/30',
    2017:'3/26,10/29',
    2018:'3/25,10/28',
    2019:'3/31,10/27'
};
s.usePlugins=true;
function s_doPlugins(s) {

    /* Make sure s.events is initialised */
    s.events = s.events ? s.events : '';

    /* s_code version */
    s.prop62 = "Guardian Membership 1.0 20140618";

    /* URL */
    s.prop61 = "D=g";

    /* Set Page View Event */
    s.events=s.apl(s.events,'event4',',',2);

    /*Email User Id - Used For Remarketing*/
    if (!s.eVar34) {
        s.eVar34=s.Util.getQueryParam('UID');
    }
    s.eVar34=s.getValOnce(s.eVar34,'s_ev34',0);

    /* Days Since Last Visit */
    s.eVar10=s.getDaysSinceLastVisit('s_lv');

    /* Most Requested Content (Prop/Var40) from the s.hier2 value */
    s.prop40=s.hier2;

    /* External Paid Campaign Tracking Only */
    if (!s.campaign) {
        s.campaign=s.Util.getQueryParam('CMP');
    }
    s.campaign=s.getValOnce(s.campaign,'s_campaign',0);
    if (s.campaign) {
        s.eVar38 = s.eVar39 = "D=v0";
    }

    /* Campaign stacking */
    s.eVar40=s.crossVisitParticipation(s.campaign,'s_ev40','30','5','>','',1);

    /* news.google and SEO Entry Pages */
    if (s.getVisitStart("s_visit") && document.referrer) {
        var referrer = document.referrer.match(/^(?:f|ht)tp(?:s)?\:\/\/([^\/]+)/i);
        if (referrer) {
            var host = referrer[1];
            var regex = new RegExp("^news\.google\.[a-z\.]{2,}$");
            if (regex.exec(host)) {
                s.prop39 = s.pageName;
            }
            else {
                var refs = ["google", "yahoo", "bing", "ask"];
                for (var i = 0; i < refs.length; ++i) {
                    regex = new RegExp("^(|[a-z0-9]+\.)" + refs[i] + "\.[a-z\.]{2,}$");
                    if (regex.exec(host)) {
                        s.prop34 = s.pageName;
                        break;
                    }
                }
            }
        }
    }

    /* Previous page name */
    if (!s.prop24) s.prop24=s.getPreviousValue(s.pageName,'gpv_pageName');
    s.eVar24="D=c24";

    /* Internal Site Search (Strip Function Applied) */
    if (s.prop22) {
        s.prop22=s.prop22.toLowerCase();
        s.eVar22=s.prop22;
        var t_search=s.getValOnce(s.eVar22,'ev22',0);
    }

    /* New/Repeat Status */
    s.prop16=s.eVar16=s.getNewRepeat(365);

    /* Set Time Parting Day and Hour Combination - 0 = GMT */
    var tpA = s.getTimeParting('n','+0');
    s.prop20 = tpA[2] + ":" + tpA[1];
    s.eVar20 = "D=c20";

    /* User Status: Registered vs. Guest Identification */
    if (s.prop2=="GUID:none" || s.prop2=="GUID:" || s.prop2=="GUID:(none)" || s.prop2=="" || s.prop2 == undefined) {
        s.prop31="guest user";
    }
    else {
        s.prop31="registered user";
    }
    if (s.prop31 && !s.eVar31) {
        s.eVar31="D=c31"; // User Status
    }

    /* Geo : Internal Search taken from VISTA Ruled prop1 and Internal search*/
    if (s.prop22){
        s.prop28=s.prop1+':'+s.prop22;
    }

    /* Internal Campaign Tracking - With Full Sub-Relations*/
    if (!s.eVar50){
        s.eVar50=s.Util.getQueryParam('INTCMP');
    }
    s.eVar50=s.getValOnce(s.eVar50,'s_intcampaign',0);

    /* Copying some props to eVars */
    if (s.prop2 && !s.eVar2) {
        s.eVar2="D=c2"; // User ID
    }
    if (s.pageName && !s.eVar7) {
        s.eVar7="D=pageName"; // Page Name in eVar
    }
    if (s.prop3 && !s.eVar3) {
        s.eVar3="D=c3";//Cottage Title
    }
    if (s.prop8 && !s.eVar8) {
        s.eVar8="D=c8"; // Page ID
    }
    if (s.prop9 && !s.eVar54) {
        s.eVar54="D=c9"; //Content Type
    }
    if (s.prop19 && !s.eVar19) {
        s.eVar19="D=c19"; //Product/Platform
    }
    if (s.channel && !s.eVar64) {
        s.eVar64="D=ch";//Channel
    }
    if (s.prop23 && !s.eVar18) {
        s.eVar18="D=c23";//Internal Search results
    }
    if (s.prop73 && !s.eVar14) {
        s.eVar14="D=c73";//Sub Sub category
    }
    if (s.prop74 && !s.eVar15) {
        s.eVar15="D=c74";//Sub Category
    }
    if (s.prop75 && !s.eVar5) {
        s.eVar5="D=c75";//Category
    }
    /* Set hier1 to the concatenation of prop19 and prop72 */
    if (s.prop19 && s.prop72) {
        s.hier3='D=c19+":"+c72';
    }
    // Previous content section
    s.prop71 = s.getPreviousValue(s.channel,"s_prev_ch");

    // Previous content type
    s.prop70 = s.getPreviousValue(s.prop9,"s_prev_prop9");

    /* Set prop63 to the load time in seconds, with one decimal value */
    s.prop63 = s.getLoadTimeDim();

    /* Navigation Interaction */
    if (s.prop41 && s.prop41!="n/a") s.events=s.apl(s.events,'event37',',',2);

    /* Kill traffic.outbrain.com as a referrer */
    var ref = s.getReferrer();
    if (ref == "traffic.outbrain.com") {
        s.referrer = "http://" + document.domain + "/outbrain";
    }

    /* Kill plugins section under Mozilla browsers (not used any more) */
    s.plugins='';

}
s.doPlugins=s_doPlugins;

/************************** PLUGINS SECTION *************************/
/* You may insert any plugins you wish to use here.                 */
s.wd=window;
/*
 * Plugin: getReferrer v1.0 - returns the hostname of external referrers
 */
s.getReferrer=new Function("",""
    +"var s=this,c,d,e,g,i,j,k,l,m,n,o;g=s.referrer?s.referrer:document.r"
    +"eferrer;g=g.toLowerCase();if(g){i=g.indexOf('?')>-1?g.indexOf('?'):"
    +"g.length;j=g.substring(0,i);k=s.linkInternalFilters.toLowerCase();k"
    +"=s.split(k,',');l=k.length;for(m=0;m<l;m++){n=j.indexOf(k[m])>-1?g:"
    +"'';if(n)o=n}if(!o){d=j.indexOf('//')>-1?j.indexOf('//')+2:0;c=g.ind"
    +"exOf('/',d)>-1?g.indexOf('/',d):j.length;e=g.substring(d,c);return "
    +"e}}");

/**
 * getLoadTimeDim v.0.1
 */
s.getLoadTimeDim=new Function("",""
    +"var t='';if(typeof performance!=='undefined'){t=(new Date()).getTim"
    +"e()-performance.timing.requestStart;t=t/1000;t=(t>=0&&t<500)?t.toFi"
    +"xed(1):'';}return t;");

/*
 * Utility Function: split v1.5 (JS 1.0 compatible)
 */
s.split=new Function("l","d",""
    +"var i,x=0,a=new Array;while(l){i=l.indexOf(d);i=i>-1?i:l.length;a[x"
    +"++]=l.substring(0,i);l=l.substring(i+d.length);}return a");

/*
 * Plugin: Days since last Visit 1.1.H - capture time from last visit
 */
s.getDaysSinceLastVisit=new Function("c",""
    +"var s=this,e=new Date(),es=new Date(),cval,cval_s,cval_ss,ct=e.getT"
    +"ime(),day=24*60*60*1000,f1,f2,f3,f4,f5;e.setTime(ct+3*365*day);es.s"
    +"etTime(ct+30*60*1000);f0='Cookies Not Supported';f1='First Visit';f"
    +"2='More than 30 days';f3='More than 7 days';f4='Less than 7 days';f"
    +"5='Less than 1 day';cval=s.c_r(c);if(cval.length==0){s.c_w(c,ct,e);"
    +"s.c_w(c+'_s',f1,es);}else{var d=ct-cval;if(d>30*60*1000){if(d>30*da"
    +"y){s.c_w(c,ct,e);s.c_w(c+'_s',f2,es);}else if(d<30*day+1 && d>7*day"
    +"){s.c_w(c,ct,e);s.c_w(c+'_s',f3,es);}else if(d<7*day+1 && d>day){s."
    +"c_w(c,ct,e);s.c_w(c+'_s',f4,es);}else if(d<day+1){s.c_w(c,ct,e);s.c"
    +"_w(c+'_s',f5,es);}}else{s.c_w(c,ct,e);cval_ss=s.c_r(c+'_s');s.c_w(c"
    +"+'_s',cval_ss,es);}}cval_s=s.c_r(c+'_s');if(cval_s.length==0) retur"
    +"n f0;else if(cval_s!=f1&&cval_s!=f2&&cval_s!=f3&&cval_s!=f4&&cval_s"
    +"!=f5) return '';else return cval_s;");

/*
 * Plugin: getVisitStart v2.0 - returns 1 on first page of visit
 * otherwise 0
 */
s.getVisitStart=new Function("c",""
    +"var s=this,v=1,t=new Date;t.setTime(t.getTime()+1800000);if(s.c_r(c"
    +")){v=0}if(!s.c_w(c,1,t)){s.c_w(c,1,0)}if(!s.c_r(c)){v=0}return v;");

/*
 *	Plug-in: crossVisitParticipation v1.7 - stacks values from
 *	specified variable in cookie and returns value
 */
s.crossVisitParticipation=new Function("v","cn","ex","ct","dl","ev","dv",""
    +"var s=this,ce;if(typeof(dv)==='undefined')dv=0;if(s.events&&ev){var"
    +" ay=s.split(ev,',');var ea=s.split(s.events,',');for(var u=0;u<ay.l"
    +"ength;u++){for(var x=0;x<ea.length;x++){if(ay[u]==ea[x]){ce=1;}}}}i"
    +"f(!v||v==''){if(ce){s.c_w(cn,'');return'';}else return'';}v=escape("
    +"v);var arry=new Array(),a=new Array(),c=s.c_r(cn),g=0,h=new Array()"
    +";if(c&&c!=''){arry=s.split(c,'],[');for(q=0;q<arry.length;q++){z=ar"
    +"ry[q];z=s.repl(z,'[','');z=s.repl(z,']','');z=s.repl(z,\"'\",'');arry"
    +"[q]=s.split(z,',')}}var e=new Date();e.setFullYear(e.getFullYear()+"
    +"5);if(dv==0&&arry.length>0&&arry[arry.length-1][0]==v)arry[arry.len"
    +"gth-1]=[v,new Date().getTime()];else arry[arry.length]=[v,new Date("
    +").getTime()];var start=arry.length-ct<0?0:arry.length-ct;var td=new"
    +" Date();for(var x=start;x<arry.length;x++){var diff=Math.round((td."
    +"getTime()-arry[x][1])/86400000);if(diff<ex){h[g]=unescape(arry[x][0"
    +"]);a[g]=[arry[x][0],arry[x][1]];g++;}}var data=s.join(a,{delim:',',"
    +"front:'[',back:']',wrap:\"'\"});s.c_w(cn,data,e);var r=s.join(h,{deli"
    +"m:dl});if(ce)s.c_w(cn,'');return r;");

/*
 * Plugin: getNewRepeat 1.2 - Returns whether user is new or repeat
 */
s.getNewRepeat=new Function("d","cn",""
    +"var s=this,e=new Date(),cval,sval,ct=e.getTime();d=d?d:30;cn=cn?cn:"
    +"'s_nr';e.setTime(ct+d*24*60*60*1000);cval=s.c_r(cn);if(cval.length="
    +"=0){s.c_w(cn,ct+'-New',e);return'New';}sval=s.split(cval,'-');if(ct"
    +"-sval[0]<30*60*1000&&sval[1]=='New'){s.c_w(cn,ct+'-New',e);return'N"
    +"ew';}else{s.c_w(cn,ct+'-Repeat',e);return'Repeat';}");

/*
 * Plugin: getValOnce_v1.1
 */
s.getValOnce=new Function("v","c","e","t",""
    +"var s=this,a=new Date,v=v?v:'',c=c?c:'s_gvo',e=e?e:0,i=t=='m'?6000"
    +"0:86400000;k=s.c_r(c);if(v){a.setTime(a.getTime()+e*i);s.c_w(c,v,e"
    +"==0?0:a);}return v==k?'':v");

/*
 * Plugin: getPreviousValue_v1.0 - return previous value of designated
 *   variable (requires split utility)
 */
s.getPreviousValue=new Function("v","c","el",""
    +"var s=this,t=new Date,i,j,r='';t.setTime(t.getTime()+1800000);if(el"
    +"){if(s.events){i=s.split(el,',');j=s.split(s.events,',');for(x in i"
    +"){for(y in j){if(i[x]==j[y]){if(s.c_r(c)) r=s.c_r(c);v?s.c_w(c,v,t)"
    +":s.c_w(c,'no value',t);return r}}}}}else{if(s.c_r(c)) r=s.c_r(c);v?"
    +"s.c_w(c,v,t):s.c_w(c,'no value',t);return r}");

/*
 * Plugin: getTimeParting 3.3
 */
s.getTimeParting=new Function("h","z",""
    +"var s=this,od;od=new Date('1/1/2000');if(od.getDay()!=6||od.getMont"
    +"h()!=0){return'Data Not Available';}else{var H,M,D,W,U,ds,de,tm,tt,"
    +"da=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Sa"
    +"turday'],d=new Date(),a=[];z=z?z:0;z=parseFloat(z);if(s._tpDST){var"
    +" dso=s._tpDST[d.getFullYear()].split(/,/);ds=new Date(dso[0]+'/'+d."
    +"getFullYear());de=new Date(dso[1]+'/'+d.getFullYear());if(h=='n'&&d"
    +">ds&&d<de){z=z+1;}else if(h=='s'&&(d>de||d<ds)){z=z+1;}}d=d.getTime"
    +"()+(d.getTimezoneOffset()*60000);d=new Date(d+(3600000*z));H=d.getH"
    +"ours();M=d.getMinutes();M=(M<10)?'0'+M:M;D=d.getDay();U='AM';W='Wee"
    +"kday';if(H>=12){U='PM';H=H-12;}if(H==0){H=12;}if(D==6||D==0){W='Wee"
    +"kend';}D=da[D];tm=H+':'+M+U;tt=H+':'+((M>30)?'30':'00')+U;a=[tm,tt,"
    +"D,W];return a;}");

/*
 * Utility Function: split v1.5 - split a string (JS 1.0 compatible)
 */
s.split=new Function("l","d",""
    +"var i,x=0,a=new Array;while(l){i=l.indexOf(d);i=i>-1?i:l.length;a[x"
    +"++]=l.substring(0,i);l=l.substring(i+d.length);}return a");

/*
 * Plugin Utility: apl v1.1
 */
s.apl=new Function("L","v","d","u",""
    +"var s=this,m=0;if(!L)L='';if(u){var i,n,a=s.split(L,d);for(i=0;i<a."
    +"length;i++){n=a[i];m=m||(u==1?(n==v):(n.toLowerCase()==v.toLowerCas"
    +"e()));}}if(!m)L=L?L+d+v:v;return L");

/*
 * s.join: 1.0 - s.join(v,p)
 *
 *  v - Array (may also be array of array)
 *  p - formatting parameters (front, back, delim, wrap)
 *
 */
s.join = new Function("v","p",""
    +"var s = this;var f,b,d,w;if(p){f=p.front?p.front:'';b=p.back?p.back"
    +":'';d=p.delim?p.delim:'';w=p.wrap?p.wrap:'';}var str='';for(var x=0"
    +";x<v.length;x++){if(typeof(v[x])=='object' )str+=s.join( v[x],p);el"
    +"se str+=w+v[x]+w;if(x<v.length-1)str+=d;}return f+str+b;");

/* Function - read combined cookies v 0.41
 * LAST UPDATED: 06-05-2013
 * APP MEASUREMENT JS COMPATIBLE
 */
if(!s.__ccucr){
    s.c_rr = s.c_r;
    s.__ccucr = true;
    function c_r(k){
        var s = this,d = new Date,v = s.c_rr(k),c = s.c_rspers(),i, m, e;
        if(v)return v;k = s.Util.urlDecode(k);i = c.indexOf(' ' + k + '=');c = i < 0 ? s.c_rr('s_sess') : c;
        i = c.indexOf(' ' + k + '=');m = i < 0 ? i : c.indexOf('|', i);
        e = i < 0 ? i : c.indexOf(';', i);m = m > 0 ? m : e;
        v = i < 0 ? '' : s.Util.urlDecode(c.substring(i + 2 + k.length, m < 0 ? c.length : m));
        return v;
    }
    function c_rspers(){
        var cv = s.c_rr("s_pers");var date = new Date().getTime();var expd = null;var cvarr = [];var vcv = "";
        if(!cv)return vcv; cvarr = cv.split(";");for(var i = 0, l = cvarr.length; i < l; i++){
            expd = cvarr[i].match(/\|([0-9]+)$/);if(expd && parseInt(expd[1]) >= date){vcv += cvarr[i] + ";";}}
        return vcv;
    }
    s.c_rspers = c_rspers;
    s.c_r = c_r;
}
/*
 * Function - write combined cookies v 0.41
 */
if(!s.__ccucw){
    s.c_wr = s.c_w;
    s.__ccucw = true;
    function c_w(k, v, e){
        var s = this,d = new Date,ht = 0,pn = 's_pers',sn = 's_sess',pc = 0,sc = 0,pv, sv, c, i, t;
        d.setTime(d.getTime() - 60000);if(s.c_rr(k))s.c_wr(k, '', d);k = s.Util.urlEncode(k);
        pv = s.c_rspers();i = pv.indexOf(' ' + k + '=');if(i > -1){
            pv = pv.substring(0, i) + pv.substring(pv.indexOf(';', i) + 1);pc = 1;}
        sv = s.c_rr(sn);i = sv.indexOf(' ' + k + '=');if(i > -1){
            sv = sv.substring(0, i) + sv.substring(sv.indexOf(';', i) + 1);sc = 1;}
        d = new Date;if(e){if(e.getTime() > d.getTime()){
            pv += ' ' + k + '=' + s.Util.urlEncode(v) + '|' + e.getTime() + ';';pc = 1;}}
        else{sv += ' ' + k + '=' + s.Util.urlEncode(v) + ';';sc = 1;}sv = sv.replace(/%00/g, '');
        pv = pv.replace(/%00/g, '');if(sc)s.c_wr(sn, sv, 0);if(pc){t = pv;
            while(t && t.indexOf(';') != -1){var t1 = parseInt(t.substring(t.indexOf('|') + 1, t.indexOf(';')));
                t = t.substring(t.indexOf(';') + 1);ht = ht < t1 ? t1 : ht;}d.setTime(ht);
            s.c_wr(pn, pv, d);}return v == s.c_r(s.Util.urlEncode(k));}
    s.c_w = c_w;
}

/*
 * Plugin Utility: Replace v1.0
 */
s.repl=new Function("x","o","n",""
    +"var i=x.indexOf(o),l=n.length;while(x&&i>=0){x=x.substring(0,i)+n+x."
    +"substring(i+o.length);i=x.indexOf(o,i+l)}return x");

/*
 * Utility manageVars v1.4 - clear variable values (requires split 1.5)
 */
s.manageVars=new Function("c","l","f",""
    +"var s=this,vl,la,vla;l=l?l:'';f=f?f:1 ;if(!s[c])return false;vl='pa"
    +"geName,purchaseID,channel,server,pageType,campaign,state,zip,events"
    +",products,transactionID';for(var n=1;n<76;n++){vl+=',prop'+n+',eVar"
    +"'+n+',hier'+n;}if(l&&(f==1||f==2)){if(f==1){vl=l;}if(f==2){la=s.spl"
    +"it(l,',');vla=s.split(vl,',');vl='';for(x in la){for(y in vla){if(l"
    +"a[x]==vla[y]){vla[y]='';}}}for(y in vla){vl+=vla[y]?','+vla[y]:'';}"
    +"}s.pt(vl,',',c,0);return true;}else if(l==''&&f==1){s.pt(vl,',',c,0"
    +");return true;}else{return false;}");
s.clearVars=new Function("t","var s=this;s[t]='';");
s.lowercaseVars=new Function("t",""
    +"var s=this;if(s[t]&&t!='events'){s[t]=s[t].toString();if(s[t].index"
    +"Of('D=')!=0){s[t]=s[t].toLowerCase();}}");
s.visitorNamespace = "guardian"
s.trackingServer = "hits.theguardian.com"
s.trackingServerSecure = "hits-secure.theguardian.com"
/*
 ============== DO NOT ALTER ANYTHING BELOW THIS LINE ! ===============

 AppMeasurement for JavaScript version: 1.2.4
 Copyright 1996-2013 Adobe, Inc. All Rights Reserved
 More info available at http://www.omniture.com
 */
function AppMeasurement(){var s=this;s.version="1.2.4";var w=window;if(!w.s_c_in)w.s_c_il=[],w.s_c_in=0;s._il=w.s_c_il;s._in=w.s_c_in;s._il[s._in]=s;w.s_c_in++;s._c="s_c";var k=w.hb;k||(k=null);var m=w,i,n;try{i=m.parent;for(n=m.location;i&&i.location&&n&&""+i.location!=""+n&&m.location&&""+i.location!=""+m.location&&i.location.host==n.host;)m=i,i=m.parent}catch(p){}s.Sa=function(s){try{console.log(s)}catch(a){}};s.ka=function(s){return""+parseInt(s)==""+s};s.replace=function(s,a,c){if(!s||s.indexOf(a)<
    0)return s;return s.split(a).join(c)};s.escape=function(b){var a,c;if(!b)return b;b=encodeURIComponent(b);for(a=0;a<7;a++)c="+~!*()'".substring(a,a+1),b.indexOf(c)>=0&&(b=s.replace(b,c,"%"+c.charCodeAt(0).toString(16).toUpperCase()));return b};s.unescape=function(b){if(!b)return b;b=b.indexOf("+")>=0?s.replace(b,"+"," "):b;try{return decodeURIComponent(b)}catch(a){}return unescape(b)};s.Ja=function(){var b=w.location.hostname,a=s.fpCookieDomainPeriods,c;if(!a)a=s.cookieDomainPeriods;if(b&&!s.da&&
    !/^[0-9.]+$/.test(b)&&(a=a?parseInt(a):2,a=a>2?a:2,c=b.lastIndexOf("."),c>=0)){for(;c>=0&&a>1;)c=b.lastIndexOf(".",c-1),a--;s.da=c>0?b.substring(c):b}return s.da};s.c_r=s.cookieRead=function(b){b=s.escape(b);var a=" "+s.d.cookie,c=a.indexOf(" "+b+"="),e=c<0?c:a.indexOf(";",c);b=c<0?"":s.unescape(a.substring(c+2+b.length,e<0?a.length:e));return b!="[[B]]"?b:""};s.c_w=s.cookieWrite=function(b,a,c){var e=s.Ja(),d=s.cookieLifetime,f;a=""+a;d=d?(""+d).toUpperCase():"";c&&d!="SESSION"&&d!="NONE"&&((f=a!=
    ""?parseInt(d?d:0):-60)?(c=new Date,c.setTime(c.getTime()+f*1E3)):c==1&&(c=new Date,f=c.getYear(),c.setYear(f+5+(f<1900?1900:0))));if(b&&d!="NONE")return s.d.cookie=b+"="+s.escape(a!=""?a:"[[B]]")+"; path=/;"+(c&&d!="SESSION"?" expires="+c.toGMTString()+";":"")+(e?" domain="+e+";":""),s.cookieRead(b)==a;return 0};s.D=[];s.C=function(b,a,c){if(s.ea)return 0;if(!s.maxDelay)s.maxDelay=250;var e=0,d=(new Date).getTime()+s.maxDelay,f=s.d.fb,g=["webkitvisibilitychange","visibilitychange"];if(!f)f=s.d.gb;
    if(f&&f=="prerender"){if(!s.N){s.N=1;for(c=0;c<g.length;c++)s.d.addEventListener(g[c],function(){var b=s.d.fb;if(!b)b=s.d.gb;if(b=="visible")s.N=0,s.delayReady()})}e=1;d=0}else c||s.q("_d")&&(e=1);e&&(s.D.push({m:b,a:a,t:d}),s.N||setTimeout(s.delayReady,s.maxDelay));return e};s.delayReady=function(){var b=(new Date).getTime(),a=0,c;for(s.q("_d")&&(a=1);s.D.length>0;){c=s.D.shift();if(a&&!c.t&&c.t>b){s.D.unshift(c);setTimeout(s.delayReady,parseInt(s.maxDelay/2));break}s.ea=1;s[c.m].apply(s,c.a);s.ea=
    0}};s.setAccount=s.sa=function(b){var a,c;if(!s.C("setAccount",arguments))if(s.account=b,s.allAccounts){a=s.allAccounts.concat(b.split(","));s.allAccounts=[];a.sort();for(c=0;c<a.length;c++)(c==0||a[c-1]!=a[c])&&s.allAccounts.push(a[c])}else s.allAccounts=b.split(",")};s.foreachVar=function(b,a){var c,e,d,f,g="";d=e="";if(s.lightProfileID)c=s.H,(g=s.lightTrackVars)&&(g=","+g+","+s.Q.join(",")+",");else{c=s.c;if(s.pe||s.linkType)if(g=s.linkTrackVars,e=s.linkTrackEvents,s.pe&&(d=s.pe.substring(0,1).toUpperCase()+
    s.pe.substring(1),s[d]))g=s[d].eb,e=s[d].cb;g&&(g=","+g+","+s.z.join(",")+",");e&&g&&(g+=",events,")}a&&(a=","+a+",");for(e=0;e<c.length;e++)d=c[e],(f=s[d])&&(!g||g.indexOf(","+d+",")>=0)&&(!a||a.indexOf(","+d+",")>=0)&&b(d,f)};s.X=function(b,a,c,e,d){var f="",g,j,w,q,i=0;b=="contextData"&&(b="c");if(a){for(g in a)if(!Object.prototype[g]&&(!d||g.substring(0,d.length)==d)&&a[g]&&(!c||c.indexOf(","+(e?e+".":"")+g+",")>=0)){w=!1;if(i)for(j=0;j<i.length;j++)g.substring(0,i[j].length)==i[j]&&(w=!0);if(!w&&
    (f==""&&(f+="&"+b+"."),j=a[g],d&&(g=g.substring(d.length)),g.length>0))if(w=g.indexOf("."),w>0)j=g.substring(0,w),w=(d?d:"")+j+".",i||(i=[]),i.push(w),f+=s.X(j,a,c,e,w);else if(typeof j=="boolean"&&(j=j?"true":"false"),j){if(e=="retrieveLightData"&&d.indexOf(".contextData.")<0)switch(w=g.substring(0,4),q=g.substring(4),g){case "transactionID":g="xact";break;case "channel":g="ch";break;case "campaign":g="v0";break;default:s.ka(q)&&(w=="prop"?g="c"+q:w=="eVar"?g="v"+q:w=="list"?g="l"+q:w=="hier"&&(g=
    "h"+q,j=j.substring(0,255)))}f+="&"+s.escape(g)+"="+s.escape(j)}}f!=""&&(f+="&."+b)}return f};s.La=function(){var b="",a,c,e,d,f,g,j,w,i="",k="",m=c="";if(s.lightProfileID)a=s.H,(i=s.lightTrackVars)&&(i=","+i+","+s.Q.join(",")+",");else{a=s.c;if(s.pe||s.linkType)if(i=s.linkTrackVars,k=s.linkTrackEvents,s.pe&&(c=s.pe.substring(0,1).toUpperCase()+s.pe.substring(1),s[c]))i=s[c].eb,k=s[c].cb;i&&(i=","+i+","+s.z.join(",")+",");k&&(k=","+k+",",i&&(i+=",events,"));s.events2&&(m+=(m!=""?",":"")+s.events2)}for(c=
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   0;c<a.length;c++){d=a[c];f=s[d];e=d.substring(0,4);g=d.substring(4);!f&&d=="events"&&m&&(f=m,m="");if(f&&(!i||i.indexOf(","+d+",")>=0)){switch(d){case "timestamp":d="ts";break;case "dynamicVariablePrefix":d="D";break;case "visitorID":d="vid";break;case "marketingCloudVisitorID":d="mid";break;case "analyticsVisitorID":d="aid";break;case "audienceManagerVisitorID":d="aamid";break;case "audienceManagerLocationHint":d="aamlh";break;case "pageURL":d="g";if(f.length>255)s.pageURLRest=f.substring(255),f=
    f.substring(0,255);break;case "pageURLRest":d="-g";break;case "referrer":d="r";break;case "vmk":case "visitorMigrationKey":d="vmt";break;case "visitorMigrationServer":d="vmf";s.ssl&&s.visitorMigrationServerSecure&&(f="");break;case "visitorMigrationServerSecure":d="vmf";!s.ssl&&s.visitorMigrationServer&&(f="");break;case "charSet":d="ce";break;case "visitorNamespace":d="ns";break;case "cookieDomainPeriods":d="cdp";break;case "cookieLifetime":d="cl";break;case "variableProvider":d="vvp";break;case "currencyCode":d=
    "cc";break;case "channel":d="ch";break;case "transactionID":d="xact";break;case "campaign":d="v0";break;case "resolution":d="s";break;case "colorDepth":d="c";break;case "javascriptVersion":d="j";break;case "javaEnabled":d="v";break;case "cookiesEnabled":d="k";break;case "browserWidth":d="bw";break;case "browserHeight":d="bh";break;case "connectionType":d="ct";break;case "homepage":d="hp";break;case "plugins":d="p";break;case "events":m&&(f+=(f!=""?",":"")+m);if(k){g=f.split(",");f="";for(e=0;e<g.length;e++)j=
    g[e],w=j.indexOf("="),w>=0&&(j=j.substring(0,w)),w=j.indexOf(":"),w>=0&&(j=j.substring(0,w)),k.indexOf(","+j+",")>=0&&(f+=(f?",":"")+g[e])}break;case "events2":f="";break;case "contextData":b+=s.X("c",s[d],i,d);f="";break;case "lightProfileID":d="mtp";break;case "lightStoreForSeconds":d="mtss";s.lightProfileID||(f="");break;case "lightIncrementBy":d="mti";s.lightProfileID||(f="");break;case "retrieveLightProfiles":d="mtsr";break;case "deleteLightProfiles":d="mtsd";break;case "retrieveLightData":s.retrieveLightProfiles&&
(b+=s.X("mts",s[d],i,d));f="";break;default:s.ka(g)&&(e=="prop"?d="c"+g:e=="eVar"?d="v"+g:e=="list"?d="l"+g:e=="hier"&&(d="h"+g,f=f.substring(0,255)))}f&&(b+="&"+d+"="+(d.substring(0,3)!="pev"?s.escape(f):f))}d=="pev3"&&s.g&&(b+=s.g)}return b};s.u=function(s){var a=s.tagName;if(""+s.pb!="undefined"||""+s.Xa!="undefined"&&(""+s.Xa).toUpperCase()!="HTML")return"";a=a&&a.toUpperCase?a.toUpperCase():"";a=="SHAPE"&&(a="");a&&((a=="INPUT"||a=="BUTTON")&&s.type&&s.type.toUpperCase?a=s.type.toUpperCase():
    !a&&s.href&&(a="A"));return a};s.ga=function(s){var a=s.href?s.href:"",c,e,d;c=a.indexOf(":");e=a.indexOf("?");d=a.indexOf("/");if(a&&(c<0||e>=0&&c>e||d>=0&&c>d))e=s.protocol&&s.protocol.length>1?s.protocol:l.protocol?l.protocol:"",c=l.pathname.lastIndexOf("/"),a=(e?e+"//":"")+(s.host?s.host:l.host?l.host:"")+(h.substring(0,1)!="/"?l.pathname.substring(0,c<0?0:c)+"/":"")+a;return a};s.F=function(b){var a=s.u(b),c,e,d="",f=0;if(a){c=b.protocol;e=b.onclick;if(b.href&&(a=="A"||a=="AREA")&&(!e||!c||c.toLowerCase().indexOf("javascript")<
    0))d=s.ga(b);else if(e)d=s.replace(s.replace(s.replace(s.replace(""+e,"\r",""),"\n",""),"\t","")," ",""),f=2;else if(a=="INPUT"||a=="SUBMIT"){if(b.value)d=b.value;else if(b.innerText)d=b.innerText;else if(b.textContent)d=b.textContent;f=3}else if(b.src&&a=="IMAGE")d=b.src;if(d)return{id:d.substring(0,100),type:f}}return 0};s.mb=function(b){for(var a=s.u(b),c=s.F(b);b&&!c&&a!="BODY";)if(b=b.parentElement?b.parentElement:b.parentNode)a=s.u(b),c=s.F(b);if(!c||a=="BODY")b=0;if(b&&(a=b.onclick?""+b.onclick:
    "",a.indexOf(".tl(")>=0||a.indexOf(".trackLink(")>=0))b=0;return b};s.Va=function(){var b,a,c=s.linkObject,e=s.linkType,d=s.linkURL,f,g;s.R=1;if(!c)s.R=0,c=s.j;if(c){b=s.u(c);for(a=s.F(c);c&&!a&&b!="BODY";)if(c=c.parentElement?c.parentElement:c.parentNode)b=s.u(c),a=s.F(c);if(!a||b=="BODY")c=0;if(c){var j=c.onclick?""+c.onclick:"";if(j.indexOf(".tl(")>=0||j.indexOf(".trackLink(")>=0)c=0}}else s.R=1;!d&&c&&(d=s.ga(c));d&&!s.linkLeaveQueryString&&(f=d.indexOf("?"),f>=0&&(d=d.substring(0,f)));if(!e&&
    d){var i=0,k=0,m;if(s.trackDownloadLinks&&s.linkDownloadFileTypes){j=d.toLowerCase();f=j.indexOf("?");g=j.indexOf("#");f>=0?g>=0&&g<f&&(f=g):f=g;f>=0&&(j=j.substring(0,f));f=s.linkDownloadFileTypes.toLowerCase().split(",");for(g=0;g<f.length;g++)(m=f[g])&&j.substring(j.length-(m.length+1))=="."+m&&(e="d")}if(s.trackExternalLinks&&!e&&(j=d.toLowerCase(),s.ja(j))){if(!s.linkInternalFilters)s.linkInternalFilters=w.location.hostname;f=0;s.linkExternalFilters?(f=s.linkExternalFilters.toLowerCase().split(","),
    i=1):s.linkInternalFilters&&(f=s.linkInternalFilters.toLowerCase().split(","));if(f){for(g=0;g<f.length;g++)m=f[g],j.indexOf(m)>=0&&(k=1);k?i&&(e="e"):i||(e="e")}}}s.linkObject=c;s.linkURL=d;s.linkType=e;if(s.trackClickMap||s.trackInlineStats)if(s.g="",c){e=s.pageName;d=1;c=c.sourceIndex;if(!e)e=s.pageURL,d=0;if(w.s_objectID)a.id=w.s_objectID,c=a.type=1;if(e&&a&&a.id&&b)s.g="&pid="+s.escape(e.substring(0,255))+(d?"&pidt="+d:"")+"&oid="+s.escape(a.id.substring(0,100))+(a.type?"&oidt="+a.type:"")+"&ot="+
    b+(c?"&oi="+c:"")}};s.Ma=function(){var b=s.R,a=s.linkType,c=s.linkURL,e=s.linkName;if(a&&(c||e))a=a.toLowerCase(),a!="d"&&a!="e"&&(a="o"),s.pe="lnk_"+a,s.pev1=c?s.escape(c):"",s.pev2=e?s.escape(e):"",b=1;s.abort&&(b=0);if(s.trackClickMap||s.trackInlineStats){a={};c=0;var d=s.cookieRead("s_sq"),f=d?d.split("&"):0,g,j,w;d=0;if(f)for(g=0;g<f.length;g++)j=f[g].split("="),e=s.unescape(j[0]).split(","),j=s.unescape(j[1]),a[j]=e;e=s.account.split(",");if(b||s.g){b&&!s.g&&(d=1);for(j in a)if(!Object.prototype[j])for(g=
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           0;g<e.length;g++){d&&(w=a[j].join(","),w==s.account&&(s.g+=(j.charAt(0)!="&"?"&":"")+j,a[j]=[],c=1));for(f=0;f<a[j].length;f++)w=a[j][f],w==e[g]&&(d&&(s.g+="&u="+s.escape(w)+(j.charAt(0)!="&"?"&":"")+j+"&u=0"),a[j].splice(f,1),c=1)}b||(c=1);if(c){d="";g=2;!b&&s.g&&(d=s.escape(e.join(","))+"="+s.escape(s.g),g=1);for(j in a)!Object.prototype[j]&&g>0&&a[j].length>0&&(d+=(d?"&":"")+s.escape(a[j].join(","))+"="+s.escape(j),g--);s.cookieWrite("s_sq",d)}}}return b};s.Na=function(){if(!s.bb){var b=new Date,
    a=m.location,c,e,d,f=d=e=c="",g="",w="",i="1.2",k=s.cookieWrite("s_cc","true",0)?"Y":"N",n="",p="",o=0;if(b.setUTCDate&&(i="1.3",o.toPrecision&&(i="1.5",c=[],c.forEach))){i="1.6";d=0;e={};try{d=new Iterator(e),d.next&&(i="1.7",c.reduce&&(i="1.8",i.trim&&(i="1.8.1",Date.parse&&(i="1.8.2",Object.create&&(i="1.8.5")))))}catch(r){}}c=screen.width+"x"+screen.height;d=navigator.javaEnabled()?"Y":"N";e=screen.pixelDepth?screen.pixelDepth:screen.colorDepth;g=s.w.innerWidth?s.w.innerWidth:s.d.documentElement.offsetWidth;
    w=s.w.innerHeight?s.w.innerHeight:s.d.documentElement.offsetHeight;b=navigator.plugins;try{s.b.addBehavior("#default#homePage"),n=s.b.nb(a)?"Y":"N"}catch(t){}try{s.b.addBehavior("#default#clientCaps"),p=s.b.connectionType}catch(u){}if(b)for(;o<b.length&&o<30;){if(a=b[o].name)a=a.substring(0,100)+";",f.indexOf(a)<0&&(f+=a);o++}s.resolution=c;s.colorDepth=e;s.javascriptVersion=i;s.javaEnabled=d;s.cookiesEnabled=k;s.browserWidth=g;s.browserHeight=w;s.connectionType=p;s.homepage=n;s.plugins=f;s.bb=1}};
    s.I={};s.loadModule=function(b,a){var c=s.I[b];if(!c){c=w["AppMeasurement_Module_"+b]?new w["AppMeasurement_Module_"+b](s):{};s.I[b]=s[b]=c;c.ua=function(){return c.wa};c.xa=function(a){if(c.wa=a)s[b+"_onLoad"]=a,s.C(b+"_onLoad",[s,c],1)||a(s,c)};try{Object.defineProperty?Object.defineProperty(c,"onLoad",{get:c.ua,set:c.xa}):c._olc=1}catch(e){c._olc=1}}a&&(s[b+"_onLoad"]=a,s.C(b+"_onLoad",[s,c],1)||a(s,c))};s.q=function(b){var a,c;for(a in s.I)if(!Object.prototype[a]&&(c=s.I[a])){if(c._olc&&c.onLoad)c._olc=
        0,c.onLoad(s,c);if(c[b]&&c[b]())return 1}return 0};s.Qa=function(){var b=Math.floor(Math.random()*1E13),a=s.visitorSampling,c=s.visitorSamplingGroup;c="s_vsn_"+(s.visitorNamespace?s.visitorNamespace:s.account)+(c?"_"+c:"");var e=s.cookieRead(c);if(a){e&&(e=parseInt(e));if(!e){if(!s.cookieWrite(c,b))return 0;e=b}if(e%1E4>v)return 0}return 1};s.J=function(b,a){var c,e,d,f,g,w;for(c=0;c<2;c++){e=c>0?s.aa:s.c;for(d=0;d<e.length;d++)if(f=e[d],(g=b[f])||b["!"+f]){if(!a&&(f=="contextData"||f=="retrieveLightData")&&
        s[f])for(w in s[f])g[w]||(g[w]=s[f][w]);s[f]=g}}};s.qa=function(b,a){var c,e,d,f;for(c=0;c<2;c++){e=c>0?s.aa:s.c;for(d=0;d<e.length;d++)f=e[d],b[f]=s[f],!a&&!b[f]&&(b["!"+f]=1)}};s.Ia=function(s){var a,c,e,d,f,g=0,w,i="",k="";if(s&&s.length>255&&(a=""+s,c=a.indexOf("?"),c>0&&(w=a.substring(c+1),a=a.substring(0,c),d=a.toLowerCase(),e=0,d.substring(0,7)=="http://"?e+=7:d.substring(0,8)=="https://"&&(e+=8),c=d.indexOf("/",e),c>0&&(d=d.substring(e,c),f=a.substring(c),a=a.substring(0,c),d.indexOf("google")>=
        0?g=",q,ie,start,search_key,word,kw,cd,":d.indexOf("yahoo.co")>=0&&(g=",p,ei,"),g&&w)))){if((s=w.split("&"))&&s.length>1){for(e=0;e<s.length;e++)d=s[e],c=d.indexOf("="),c>0&&g.indexOf(","+d.substring(0,c)+",")>=0?i+=(i?"&":"")+d:k+=(k?"&":"")+d;i&&k?w=i+"&"+k:k=""}c=253-(w.length-k.length)-a.length;s=a+(c>0?f.substring(0,c):"")+"?"+w}return s};s.za=!1;s.$=!1;s.kb=function(b){s.marketingCloudVisitorID=b;s.$=!0;s.A()};s.K=!1;s.Y=!1;s.ta=function(b){s.analyticsVisitorID=b;s.Y=!0;s.A()};s.ya=!1;s.Z=!1;
    s.jb=function(b){s.audienceManagerVisitorID=b;if(s.audienceManagerVisitorID&&s.visitor.getAudienceManagerLocationHint)s.audienceManagerLocationHint=s.visitor.getAudienceManagerLocationHint();s.Z=!0;s.A()};s.isReadyToTrack=function(){var b=!0,a=s.visitor;if(a&&a.isAllowed()){if(!s.K&&!s.analyticsVisitorID&&a.getAnalyticsVisitorID&&(s.analyticsVisitorID=a.getAnalyticsVisitorID([s,s.ta]),!s.analyticsVisitorID))s.K=!0;if(s.za&&!s.$&&!s.marketingCloudVisitorID||s.K&&!s.Y&&!s.analyticsVisitorID||s.ya&&
        !s.Z&&!s.audienceManagerVisitorID)b=!1}return b};s.k=k;s.l=0;s.callbackWhenReadyToTrack=function(b,a,c){var e;e={};e.Da=b;e.Ca=a;e.Aa=c;if(s.k==k)s.k=[];s.k.push(e);if(s.l==0)s.l=setInterval(s.A,100)};s.A=function(){var b;if(s.isReadyToTrack()){if(s.l)clearInterval(s.l),s.l=0;if(s.k!=k)for(;s.k.length>0;)b=s.k.shift(),b.Ca.apply(b.Da,b.Aa)}};s.va=function(b){var a,c,e=k,d=k;if(!s.isReadyToTrack()){a=[];if(b!=k)for(c in e={},b)e[c]=b[c];d={};s.qa(d,!0);a.push(e);a.push(d);s.callbackWhenReadyToTrack(s,
        s.track,a);return!0}return!1};s.Ka=function(){var b=s.cookieRead("s_fid"),a="",c="",e;e=8;var d=4;if(!b||b.indexOf("-")<0){for(b=0;b<16;b++)e=Math.floor(Math.random()*e),a+="0123456789ABCDEF".substring(e,e+1),e=Math.floor(Math.random()*d),c+="0123456789ABCDEF".substring(e,e+1),e=d=16;b=a+"-"+c}s.cookieWrite("s_fid",b,1)||(b=0);return b};s.t=s.track=function(b,a){var c,e=new Date,d="s"+Math.floor(e.getTime()/108E5)%10+Math.floor(Math.random()*1E13),f=e.getYear();f="t="+s.escape(e.getDate()+"/"+e.getMonth()+
        "/"+(f<1900?f+1900:f)+" "+e.getHours()+":"+e.getMinutes()+":"+e.getSeconds()+" "+e.getDay()+" "+e.getTimezoneOffset());s.q("_s");if(!s.C("track",arguments)){if(!s.va(b)){a&&s.J(a);b&&(c={},s.qa(c,0),s.J(b));if(s.Qa()){if(!s.analyticsVisitorID&&!s.marketingCloudVisitorID)s.fid=s.Ka();s.Va();s.usePlugins&&s.doPlugins&&s.doPlugins(s);if(s.account){if(!s.abort){if(s.trackOffline&&!s.timestamp)s.timestamp=Math.floor(e.getTime()/1E3);e=w.location;if(!s.pageURL)s.pageURL=e.href?e.href:e;if(!s.referrer&&
        !s.ra)s.referrer=m.document.referrer,s.ra=1;s.referrer=s.Ia(s.referrer);s.q("_g")}s.Ma()&&!s.abort&&(s.Na(),f+=s.La(),s.Ua(d,f));s.abort||s.q("_t")}}b&&s.J(c,1)}s.timestamp=s.linkObject=s.j=s.linkURL=s.linkName=s.linkType=w.ob=s.pe=s.pev1=s.pev2=s.pev3=s.g=0}};s.tl=s.trackLink=function(b,a,c,e,d){s.linkObject=b;s.linkType=a;s.linkName=c;if(d)s.i=b,s.p=d;return s.track(e)};s.trackLight=function(b,a,c,e){s.lightProfileID=b;s.lightStoreForSeconds=a;s.lightIncrementBy=c;return s.track(e)};s.clearVars=
        function(){var b,a;for(b=0;b<s.c.length;b++)if(a=s.c[b],a.substring(0,4)=="prop"||a.substring(0,4)=="eVar"||a.substring(0,4)=="hier"||a.substring(0,4)=="list"||a=="channel"||a=="events"||a=="eventList"||a=="products"||a=="productList"||a=="purchaseID"||a=="transactionID"||a=="state"||a=="zip"||a=="campaign")s[a]=void 0};s.Ua=function(b,a){var c,e=s.trackingServer;c="";var d=s.dc,f="sc.",g=s.visitorNamespace;if(e){if(s.trackingServerSecure&&s.ssl)e=s.trackingServerSecure}else{if(!g)g=s.account,e=g.indexOf(","),
        e>=0&&(g=g.ib(0,e)),g=g.replace(/[^A-Za-z0-9]/g,"");c||(c="2o7.net");d=d?(""+d).toLowerCase():"d1";c=="2o7.net"&&(d=="d1"?d="112":d=="d2"&&(d="122"),f="");e=g+"."+d+"."+f+c}c=s.ssl?"https://":"http://";c+=e+"/b/ss/"+s.account+"/"+(s.mobile?"5.":"")+"1/JS-"+s.version+(s.ab?"T":"")+"/"+b+"?AQB=1&ndh=1&"+a+"&AQE=1";s.Pa&&(c=c.substring(0,2047));s.Ga(c);s.O()};s.Ga=function(b){s.e||s.Oa();s.e.push(b);s.P=s.r();s.pa()};s.Oa=function(){s.e=s.Ra();if(!s.e)s.e=[]};s.Ra=function(){var b,a;if(s.U()){try{(a=
        w.localStorage.getItem(s.S()))&&(b=w.JSON.parse(a))}catch(c){}return b}};s.U=function(){var b=!0;if(!s.trackOffline||!s.offlineFilename||!w.localStorage||!w.JSON)b=!1;return b};s.ha=function(){var b=0;if(s.e)b=s.e.length;s.v&&b++;return b};s.O=function(){if(!s.v)if(s.ia=k,s.T)s.P>s.G&&s.na(s.e),s.W(500);else{var b=s.Ba();if(b>0)s.W(b);else if(b=s.fa())s.v=1,s.Ta(b),s.Ya(b)}};s.W=function(b){if(!s.ia)b||(b=0),s.ia=setTimeout(s.O,b)};s.Ba=function(){var b;if(!s.trackOffline||s.offlineThrottleDelay<=
        0)return 0;b=s.r()-s.ma;if(s.offlineThrottleDelay<b)return 0;return s.offlineThrottleDelay-b};s.fa=function(){if(s.e.length>0)return s.e.shift()};s.Ta=function(b){if(s.debugTracking){var a="AppMeasurement Debug: "+b;b=b.split("&");var c;for(c=0;c<b.length;c++)a+="\n\t"+s.unescape(b[c]);s.Sa(a)}};s.Ya=function(b){var a;if(!a)a=new Image,a.alt="";a.ca=function(){try{if(s.V)clearTimeout(s.V),s.V=0;if(a.timeout)clearTimeout(a.timeout),a.timeout=0}catch(b){}};a.onload=a.$a=function(){a.ca();s.Fa();s.L();
        s.v=0;s.O()};a.onabort=a.onerror=a.Ha=function(){a.ca();(s.trackOffline||s.T)&&s.v&&s.e.unshift(s.Ea);s.v=0;s.P>s.G&&s.na(s.e);s.L();s.W(500)};a.onreadystatechange=function(){a.readyState==4&&(a.status==200?a.$a():a.Ha())};s.ma=s.r();a.src=b;if(a.abort)s.V=setTimeout(a.abort,5E3);s.Ea=b;s.lb=w["s_i_"+s.replace(s.account,",","_")]=a;if(s.useForcedLinkTracking&&s.B||s.p){if(!s.forcedLinkTrackingTimeout)s.forcedLinkTrackingTimeout=250;s.M=setTimeout(s.L,s.forcedLinkTrackingTimeout)}};s.Fa=function(){if(s.U()&&
        !(s.la>s.G))try{w.localStorage.removeItem(s.S()),s.la=s.r()}catch(b){}};s.na=function(b){if(s.U()){s.pa();try{w.localStorage.setItem(s.S(),w.JSON.stringify(b)),s.G=s.r()}catch(a){}}};s.pa=function(){if(s.trackOffline){if(!s.offlineLimit||s.offlineLimit<=0)s.offlineLimit=10;for(;s.e.length>s.offlineLimit;)s.fa()}};s.forceOffline=function(){s.T=!0};s.forceOnline=function(){s.T=!1};s.S=function(){return s.offlineFilename+"-"+s.visitorNamespace+s.account};s.r=function(){return(new Date).getTime()};s.ja=
        function(s){s=s.toLowerCase();if(s.indexOf("#")!=0&&s.indexOf("about:")!=0&&s.indexOf("opera:")!=0&&s.indexOf("javascript:")!=0)return!0;return!1};s.setTagContainer=function(b){var a,c,e;s.ab=b;for(a=0;a<s._il.length;a++)if((c=s._il[a])&&c._c=="s_l"&&c.tagContainerName==b){s.J(c);if(c.lmq)for(a=0;a<c.lmq.length;a++)e=c.lmq[a],s.loadModule(e.n);if(c.ml)for(e in c.ml)if(s[e])for(a in b=s[e],e=c.ml[e],e)if(!Object.prototype[a]&&(typeof e[a]!="function"||(""+e[a]).indexOf("s_c_il")<0))b[a]=e[a];if(c.mmq)for(a=
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         0;a<c.mmq.length;a++)e=c.mmq[a],s[e.m]&&(b=s[e.m],b[e.f]&&typeof b[e.f]=="function"&&(e.a?b[e.f].apply(b,e.a):b[e.f].apply(b)));if(c.tq)for(a=0;a<c.tq.length;a++)s.track(c.tq[a]);c.s=s;break}};s.Util={urlEncode:s.escape,urlDecode:s.unescape,cookieRead:s.cookieRead,cookieWrite:s.cookieWrite,getQueryParam:function(b,a,c){var e;a||(a=s.pageURL?s.pageURL:w.location);c||(c="&");if(b&&a&&(a=""+a,e=a.indexOf("?"),e>=0&&(a=c+a.substring(e+1)+c,e=a.indexOf(c+b+"="),e>=0&&(a=a.substring(e+c.length+b.length+
        1),e=a.indexOf(c),e>=0&&(a=a.substring(0,e)),a.length>0))))return s.unescape(a);return""}};s.z=["timestamp","dynamicVariablePrefix","visitorID","marketingCloudVisitorID","analyticsVisitorID","audienceManagerVisitorID","audienceManagerLocationHint","fid","vmk","visitorMigrationKey","visitorMigrationServer","visitorMigrationServerSecure","charSet","visitorNamespace","cookieDomainPeriods","fpCookieDomainPeriods","cookieLifetime","pageName","pageURL","referrer","contextData","currencyCode","lightProfileID",
        "lightStoreForSeconds","lightIncrementBy","retrieveLightProfiles","deleteLightProfiles","retrieveLightData","pe","pev1","pev2","pev3","pageURLRest"];s.c=s.z.concat(["purchaseID","variableProvider","channel","server","pageType","transactionID","campaign","state","zip","events","events2","products","tnt"]);s.Q=["timestamp","charSet","visitorNamespace","cookieDomainPeriods","cookieLifetime","contextData","lightProfileID","lightStoreForSeconds","lightIncrementBy"];s.H=s.Q.slice(0);s.aa=["account","allAccounts",
        "debugTracking","visitor","trackOffline","offlineLimit","offlineThrottleDelay","offlineFilename","usePlugins","doPlugins","configURL","visitorSampling","s.visitorSamplingGroup","linkObject","linkURL","linkName","linkType","trackDownloadLinks","trackExternalLinks","trackClickMap","trackInlineStats","linkLeaveQueryString","linkTrackVars","linkTrackEvents","linkDownloadFileTypes","linkExternalFilters","linkInternalFilters","useForcedLinkTracking","forcedLinkTrackingTimeout","trackingServer","trackingServerSecure",
        "ssl","abort","mobile","dc","lightTrackVars","maxDelay"];for(i=0;i<=75;i++)s.c.push("prop"+i),s.H.push("prop"+i),s.c.push("eVar"+i),s.H.push("eVar"+i),i<6&&s.c.push("hier"+i),i<4&&s.c.push("list"+i);i=["resolution","colorDepth","javascriptVersion","javaEnabled","cookiesEnabled","browserWidth","browserHeight","connectionType","homepage","plugins"];s.c=s.c.concat(i);s.z=s.z.concat(i);s.ssl=w.location.protocol.toLowerCase().indexOf("https")>=0;s.charSet="UTF-8";s.contextData={};s.offlineThrottleDelay=
        0;s.offlineFilename="AppMeasurement.offline";s.ma=0;s.P=0;s.G=0;s.la=0;s.linkDownloadFileTypes="exe,zip,wav,mp3,mov,mpg,avi,wmv,pdf,doc,docx,xls,xlsx,ppt,pptx";s.w=w;s.d=w.document;try{s.Pa=navigator.appName=="Microsoft Internet Explorer"}catch(o){}s.L=function(){if(s.M)w.clearTimeout(s.M),s.M=k;s.i&&s.B&&s.i.dispatchEvent(s.B);if(s.p)if(typeof s.p=="function")s.p();else if(s.i&&s.i.href)s.d.location=s.i.href;s.i=s.B=s.p=0};s.oa=function(){s.b=s.d.body;if(s.b)if(s.o=function(b){var a,c,e,d,f;if(!(s.d&&
        s.d.getElementById("cppXYctnr")||b&&b.Wa)){if(s.ba)if(s.useForcedLinkTracking)s.b.removeEventListener("click",s.o,!1);else{s.b.removeEventListener("click",s.o,!0);s.ba=s.useForcedLinkTracking=0;return}else s.useForcedLinkTracking=0;s.j=b.srcElement?b.srcElement:b.target;try{if(s.j&&(s.j.tagName||s.j.parentElement||s.j.parentNode))if(e=s.ha(),s.track(),e<s.ha()&&s.useForcedLinkTracking&&b.target){for(d=b.target;d&&d!=s.b&&d.tagName.toUpperCase()!="A"&&d.tagName.toUpperCase()!="AREA";)d=d.parentNode;
        if(d&&(f=d.href,s.ja(f)||(f=0),c=d.target,b.target.dispatchEvent&&f&&(!c||c=="_self"||c=="_top"||c=="_parent"||w.name&&c==w.name))){try{a=s.d.createEvent("MouseEvents")}catch(g){a=new w.MouseEvent}if(a){try{a.initMouseEvent("click",b.bubbles,b.cancelable,b.view,b.detail,b.screenX,b.screenY,b.clientX,b.clientY,b.ctrlKey,b.altKey,b.shiftKey,b.metaKey,b.button,b.relatedTarget)}catch(i){a=0}if(a)a.Wa=1,b.stopPropagation(),b.Za&&b.Za(),b.preventDefault(),s.i=b.target,s.B=a}}}}catch(k){}s.j=0}},s.b&&s.b.attachEvent)s.b.attachEvent("onclick",
        s.o);else{if(s.b&&s.b.addEventListener){if(navigator&&(navigator.userAgent.indexOf("WebKit")>=0&&s.d.createEvent||navigator.userAgent.indexOf("Firefox/2")>=0&&w.MouseEvent))s.ba=1,s.useForcedLinkTracking=1,s.b.addEventListener("click",s.o,!0);s.b.addEventListener("click",s.o,!1)}}else setTimeout(s.oa,30)};s.oa()}
function s_gi(s){var w,k=window.s_c_il,m,i=s.split(","),n,p,o=0;if(k)for(m=0;!o&&m<k.length;){w=k[m];if(w._c=="s_c"&&w.account)if(w.account==s)o=1;else{if(!w.allAccounts)w.allAccounts=w.account.split(",");for(n=0;n<i.length;n++)for(p=0;p<w.allAccounts.length;p++)i[n]==w.allAccounts[p]&&(o=1)}m++}o||(w=new AppMeasurement);w.setAccount(s);return w}AppMeasurement.getInstance=s_gi;window.s_objectID||(window.s_objectID=0);
function s_pgicq(){var s=window,w=s.s_giq,k,m,i;if(w)for(k=0;k<w.length;k++)m=w[k],i=s_gi(m.oun),i.setAccount(m.un),i.setTagContainer(m.tagContainerName);s.s_giq=0}s_pgicq();
