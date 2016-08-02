package app.hongs.db.util;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.db.link.Loop;
import app.hongs.util.Synt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关联查询
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x10c0~0x10cf
 * 0x10c0 获取行号失败, 可能缺少关联字段
 * 后续号码为 FetchMost 的
 * </pre>
 *
 * @author Hongs
 */
public class FetchMore
{

  protected List<Map> rows;

  public FetchMore(List<Map> rows)
  {
    this.rows = rows;
  }

  /**
   * 获取关联ID和行
   *
   * @param map
   * @param keys
   */
  private void maping(Map<String, List> map, List rows, String... keys)
  {
    Iterator it = rows.iterator();
    W:while (it.hasNext())
    {
      Object row = it.next();
      Object obj = row;

      // 获取id值
      for (int i = 0; i < keys.length; i ++)
      {
        if (obj instanceof Map )
        {
          obj = ((Map)obj).get(keys[i]);
        }
        else
        if (obj instanceof List)
        {
          // 切割子键数组
          int j = keys.length - i ;
          String[] keyz = new String[j];
          System.arraycopy(keys,i, keyz,0,j);

          // 往下递归一层
          this.maping(map, (List) obj, keyz);

          continue W;
        }
        else
        {
          continue W;
        }
      }
      if (obj == null)
      {
          continue;
      }

      // 登记行
      String str = obj.toString( );
      if (map.containsKey(str))
      {
        map.get(str ).add(row);
      }
      else
      {
        List lst = new ArrayList();
        map.put(str , lst);
        lst.add(row);
      }
    }
  }

  public Map<String, List> maping(String... keys) {
    Map<String, List> map = new HashMap();
    maping(map, rows, keys);
    return map;
  }

  public Map<String, List> mapped(String key) {
    Map<String, List> map = new HashMap();
    maping(map, rows, key.split( "\\." ));
    return map;
  }

  /**
   * 获取关联数据
   * 类似 SQL: JOIN table ON table.col = super.key
   * 注意 app.hogns.dh.MergeMore 的 col,key 参数顺序与此相反, 下同
   * @param table 关联表
   * @param caze  附加查询
   * @param col   关联字段
   * @param key   映射键名
   * @throws app.hongs.HongsException
   */
  public void join(Table table, FetchCase caze, String col, String key)
    throws HongsException
  {
    if (this.rows.isEmpty())
    {
      return;
    }

    DB db = table.db;
    String       name   = table.name;
    String  tableName   = table.tableName;
    boolean multi       = caze.getOption("ASSOC_MULTI", false);
    boolean merge       = caze.getOption("ASSOC_MERGE", false);

    if (null != caze.name && 0 != caze.name.length())
    {
        name  = caze.name;
    }

    // 获取id及行号
    Map<String, List> map = this.mapped(key);
    Set ids = map.keySet();
    if (ids.isEmpty())
    {
      //throw new HongsException(0x10c0, "Ids map is empty");
      return;
    }

    // 识别字段别名
    String rel = col;
    if (table.getFields().containsKey(col))
    {
      col = ".`" + col + "`";
    }
    else
    {
      Pattern pattern;
      Matcher matcher;
      do
      {
        pattern = Pattern.compile(
            "^(.+?)(?:\\s+AS)?\\s+`?(.+?)`?$",
            Pattern.CASE_INSENSITIVE );
        matcher = pattern.matcher(col);
        if (matcher.find())
        {
          col = matcher.group(1);
          rel = matcher.group(2);
          break;
        }

        pattern = Pattern.compile(
            "^(.+?)\\.\\s*`?(.+?)`?$");
        matcher = pattern.matcher(col);
        if (matcher.find())
        {
          col = matcher.group(0);
          rel = matcher.group(2);
          break;
        }
      }
      while (false);
    }

    // 构建查询结构
    caze.where(col + " IN (?)", ids)
        .from (tableName , name);

    // 获取关联数据
    Loop rs = db.queryMore(caze);

    /**
     * 根据之前的 id=>行 关系以表名为键放入列表中
     */

    Map     row, sub;
    List    lst;
    String  sid;

    if (! multi)
    {
      while ((sub = rs.next()) != null)
      {
        sid = Synt.declare(sub.get(rel), String.class);
        lst = map.get(sid);

        if (lst == null)
        {
          //throw new HongsException(0x10c0, "Line nums is null");
          continue;
        }

        Iterator it = lst.iterator();
        while (it.hasNext())
        {
          row = (Map) it.next();

          if (! merge)
          {
            row.put(name, sub);
          }
          else
          {
            sub.putAll(row);
            row.putAll(sub);
          }
        }
      }
    }
    else
    {
      while ((sub = rs.next()) != null)
      {
        sid = Synt.declare(sub.get(rel), String.class);
        lst = map.get(sid);

        if (lst == null)
        {
          //throw new HongsException(0x10c0, "Line nums is null");
          continue;
        }

        Iterator it = lst.iterator();
        while (it.hasNext())
        {
          row = (Map) it.next();

          if (row.containsKey(name))
          {
            (( List ) row.get(name)).add(sub);
          }
          else
          {
            List lzt = new ArrayList();
            row.put(name, lzt);
            lzt.add(sub);
          }
        }
      }
    }
  }

  /**
   * 获取关联数据
   *
   * @param table 关联表
   * @param col   关联字段
   * @param key   映射键名
   * @throws app.hongs.HongsException
   */
  public void join(Table table, String col, String key)
    throws HongsException
  {
    this.join(table, new FetchCase( ), col, key);
  }

  /**
   * 获取关联数据
   *
   * @param table 关联表
   * @param col   关联字段
   * @throws app.hongs.HongsException
   */
  public void join(Table table, String col)
    throws HongsException
  {
    this.join(table, new FetchCase( ), col, col);
  }

  //** 静态方法 **/

}