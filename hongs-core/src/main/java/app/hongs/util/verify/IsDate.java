package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.util.Synt;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期校验
 * <pre>
 * 规则参数:
 *  type    日期类型, date(Date对象),time(毫秒数),datestamp(Date对象,精确到秒),timestamp(时间戳,精确到秒)
 *  format  日期格式, 同 java 的 SimpleDateFormat, 默认从语言资源中提取
 *  offset  偏移时间, 毫秒时间戳, 可用 +-, 与 format 配合来解决日期精度
 *  min     最小时间, 毫秒时间戳, 可用 +- 前缀表示当前时间偏移
 *  max     最大时间, 毫秒时间戳, 可用 +- 前缀表示当前时间偏移
 * </pre>
 * @author Hongs
 */
public class IsDate extends Rule {
    @Override
    public Object verify(Object value) throws Wrong, HongsException {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        String typa = Synt.declare(params.get("__type__"), "");
        String type = Synt.declare(params.get(  "type"  ), "");

        // 日期格式
        int     off = Synt.declare(params.get( "offset" ), 0 );
        String  fmt = Synt.declare(params.get( "format" ), "");
        String  fwt =  fmt ;
        if ( "".equals(fmt)) {
            fwt = CoreLocale.getInstance().getProperty("core.default." + typa + ".format");
            if (fwt == null) {
                throw new HongsException.Common("Can not recognize date type '"+typa+"'.");
            }
        }

        Date day = getDate( value, typa, type, fwt, off );

        // 区间校验
        String  min = Synt.declare(params.get(   "min"  ), "");
        String  max = Synt.declare(params.get(   "max"  ), "");
        long now = new Date().getTime();
        if (!"".equals(min)) {
            long tim = getTime(min, now);
            if ( tim > day.getTime( ) ) {
                throw new Wrong("fore.form.lt.mindate", new SimpleDateFormat(fwt).format(new Date(tim)));
            }
        }
        if (!"".equals(max)) {
            long tim = getTime(max, now);
            if ( tim < day.getTime( ) ) {
                throw new Wrong("fore.form.gt.maxdate", new SimpleDateFormat(fwt).format(new Date(tim)));
            }
        }

        if ("date".equals(type)) {
            return day;
        }
        if ("time".equals(type)) {
            return day.getTime();
        }
        if ("datestamp".equals(type)) {
            return day;
        }
        if ("timestamp".equals(type)) {
            return day.getTime() / 1000;
        }
        if (!"".equals(fmt)) {
            return new SimpleDateFormat(fmt).format(day);
        }

        return value;
    }

    private Date getDate(Object val, String typa, String type, String fwt, int off) throws Wrong {
        if (val instanceof Date) {
            return  (Date) val ;
        }

        if (val instanceof Number) {
            long num = Synt.declare(val,0L);
            if ("timestamp".equals(type)
            ||  "datestamp".equals(type)) {
                return new Date(num * 1000);
            } else {
                return new Date(num /* */ );
            }
        }

        if (val instanceof String) {
            String str =  (String) val;
            if (Pattern.matches("^\\d+$", str)) {
                long num = Synt.declare(val,0L);
                if ("timestamp".equals(type)
                ||  "datestamp".equals(type)) {
                    return new Date(num * 1000);
                } else {
                    return new Date(num /* */ );
                }
            }

            // 按指定格式解析日期字符串
            // 要精确时间的可以使用偏移
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fwt);
                Calendar cal = Calendar.getInstance();
                TimeZone tmz = Core.getTimezone( );
                sdf.setTimeZone(tmz);
                cal.setTimeZone(tmz);
                cal.setTime(sdf.parse(str));
                cal.add(Calendar.MILLISECOND, off);
                return cal.getTime();
            } catch (ParseException e) {
                // Nothing todo.
            }
        }

        throw new Wrong("fore.form.is.not."+ typa);
    }

    private long getTime(String tim, long now) throws HongsException {
        Matcher mat = Pattern.compile("^([+-])?(\\d+)$").matcher(tim);
        if (!mat.matches()) {
            throw new HongsException.Common("Can not recognize time '"+tim+"'.");
        }
        long    msc = Synt.declare(mat.group(2), 0L);
        String  sym = mat.group(1);
        if ("+".equals(sym)) {
            return now + msc;
        } else
        if ("-".equals(sym)) {
            return now - msc;
        } else {
            return msc;
        }
    }
}
