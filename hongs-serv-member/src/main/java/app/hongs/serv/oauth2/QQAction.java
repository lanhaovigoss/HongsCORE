package app.hongs.serv.oauth2;

import app.hongs.serv.auth.ConnKit;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.serv.auth.AuthKit;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * QQ关联登录
 * @author Hongs
 */
@Action("handle/oauth2/qq")
public class QQAction {

    /**
     * QQ Web 登录回调
     * @param helper
     * @throws HongsException 
     */
    @Action("web/create")
    public void inWeb(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.qq.web.app.id" );
        String  appSk = cc.getProperty("oauth2.qq.web.app.key");
        String   rurl = cc.getProperty("oauth2.qq.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk, rurl, false);
        String  opnId = (String) info.get("opnid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wx", appId, opnId, name, head, System.currentTimeMillis());

        ConnKit.redirect(helper);
    }

    /**
     * QQ WAP 登录回调
     * @param helper
     * @throws HongsException 
     */
    @Action("wap/create")
    public void inWap(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.qq.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.qq.wap.app.key");
        String   rurl = cc.getProperty("oauth2.qq.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk, rurl, true );
        String  opnId = (String) info.get("opnid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wx", appId, opnId, name, head, System.currentTimeMillis());

        ConnKit.redirect(helper);
    }

    public static Map getUserInfo(String code, String appId, String appSk, String rurl, boolean inQQ)
    throws HongsException {
        Map    req;
        Map    rsp;
        int    err;
        String url;
        String token;
        String opnId;

        url = inQQ
            ? "https://graph.z.qq.com/moc2/token"
            : "https://graph.qq.com/oauth2.0/token";
        req = new HashMap();
        req.put("code"          , code );
        req.put("client_id"     , appId);
        req.put("client_secret" , appSk);
        req.put("redirect_uri"  , rurl );
        req.put("grant_type"    , "authorization_code");
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("code"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get token error\r\n"+Data.toString(rsp));
        }
        token = (String) rsp.get("access_token");

        url = inQQ
            ? "https://graph.z.qq.com/moc2/me"
            : "https://graph.qq.com/oauth2.0/me";
        req = new HashMap();
        req.put("access_token"  , token);
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("code"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get opnId error\r\n"+Data.toString(rsp));
        }
        opnId = (String) rsp.get("openid");

        url = "https://graph.qq.com/user/get_user_info";
        req = new HashMap();
        req.put("oauth_consumer_key", appId);
        req.put("access_token"  , token);
        req.put("openid"        , opnId);
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("ret"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get user info error\r\n"+Data.toString(rsp));
        }

        req = new HashMap();
        req.put("appid", "qq" );
        req.put("opnid", opnId);
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("figureurl_qq_2"));

        return req;
    }

}
