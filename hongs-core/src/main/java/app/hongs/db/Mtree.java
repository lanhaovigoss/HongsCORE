package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.db.util.FetchCase;
import app.hongs.db.util.FetchMore;
import app.hongs.util.Synt;

import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 树形模型
 *
 * <p>
 * 用于与树JS组件进行交互
 * </p>
 *
 * <h3>URL参数说明:</h3>
 * <pre>
 * pid          获取pid 指定的一组节点
 * id[]         获取id[]指定的全部节点
 * md           4附带路径ID, 8附带路径信息
 * </pre>
 *
 * <h3>JS请求参数组合:</h3>
 * <pre>
 * 获取层级: ?pid=xxx
 * 获取节点: ?id=xxx&md=8
 * 查找节点: ?wd=xxx&md=4
 * </pre>
 *
 * @author Hong
 */
public class Mtree extends Model
{

  /**
   * 根节点id
   * 用于确认是路径深度
   */
  public String rootId  = "0";

  /**
   * 参考id参数名
   * 虚拟id, 影响put, 用于移动节点时指定顺序
   */
  public String bidKey  = "bid";

  /**
   * 父级id字段名
   */
  public String pidKey  = "pid";

  /**
   * 名称字段名
   */
  public String nameKey = "name";

  /**
   * 说明字段名(非必要)
   */
  public String noteKey =  null ;

  /**
   * 类型字段名(非必要)
   */
  public String typeKey =  null ;

  /**
   * 子数目字段名(非必要)
   */
  public String cnumKey =  null ;

  /**
   * 排序号字段名(非必要)
   */
  public String snumKey =  null ;

  /**
   * 构造方法
   *
   * 需指定该模型对应的表对象.
   * 如传递的 bid,pid 等参数名不同,
   * 或 name,root,pid 等字段名不同,
   * 可在构造时分别指定;
   * 请指定被搜索的字段.
   *
   * @param table
   * @throws app.hongs.HongsException
   */
  public Mtree(Table table)
    throws HongsException
  {
    super( table );

    // 从表配置提取参数
    this.rootId  = Synt.defoult(table.getState("root.id"), "0");
    this.bidKey  = Synt.defoult(table.getField("bid" ), "bid" );
    this.pidKey  = Synt.defoult(table.getField("pid" ), "pid" );
    this.nameKey = Synt.defoult(table.getField("name"), "name");
    this.noteKey = table.getField("note");
    this.typeKey = table.getField("type");
    this.cnumKey = table.getField("cnum");
    this.snumKey = table.getField("snum");
  }

  /**
   * 构造方法
   *
   * 同 Mtree(Table)
   * 当临时动态从 Model 取 Mtree 时可使用此构造方法
   * 然后可动态将 rootId,nameKey 等关键字段重新设置
   *
   * 需要注意的是:
   * 如果 model 中覆盖了 add,put,del和filter 等
   * 调用 Mtree 中相同方法并不会使用此 model 的
   *
   * @param model
   * @throws HongsException
   */
  public Mtree(Model model)
    throws HongsException
  {
    this ( model.table );
  }

  //** 标准动作方法 **/

  @Override
  public Map getInfo(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap();
    }

    Map info = super.getInfo(rd, caze);

    //** 附带路径 **/

    if (info.isEmpty())
    {
        return info;
    }

    byte   pth = Synt.declare(rd.get("md"),(byte)  0 );
    String pid = Synt.declare(rd.get(this.pidKey), "");
    if (pid.length() == 0)
    {
        pid = this.rootId;
    }

    if (4 == (4 & pth))
    {
      String id = (String) info.get(this.table.primaryKey);
      info.put("path", this.getParentIds(id, pid));
    }
    else
    if (8 == (8 & pth))
    {
      String id = (String) info.get(this.table.primaryKey);
      info.put("path", this.getParents  (id, pid));
    }

    return info;
  }

  @Override
  public Map getInfo(Map rd)
    throws HongsException
  {
    return this.getInfo(rd, null);
  }

  /**
   * 获取列表
   *
   * 与 Model.getList 不同
   * 不给 colsKey 参数时仅获取基础字段
   * 不给 rowsKey 参数时不进行分页查询
   *
   * @param rd
   * @param caze
   * @return 树列表
   * @throws app.hongs.HongsException
   */
  @Override
  public Map getList(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap();
    }
    if (caze == null)
    {
      caze = fetchCase();
    }

    //** 默认字段 **/

    if (!caze.hasField() && !rd.containsKey(Cnst.RB_KEY))
    {
      if (!caze.hasOption("ASSOCS")
      &&  !caze.hasOption("ASSOC_TYPES")
      &&  !caze.hasOption("ASSOC_JOINS"))
      {
        caze.setOption("ASSOCS", new HashSet());
      }

      caze.select("`"+this.table.name+"`.`" + this.table.primaryKey + "`")
          .select("`"+this.table.name+"`.`" + this.pidKey  + "`")
          .select("`"+this.table.name+"`.`" + this.nameKey + "`");

      if (this.noteKey != null)
      {
        caze.select("`"+this.table.name+"`.`" + this.noteKey + "`");
      }
      if (this.typeKey != null)
      {
        caze.select("`"+this.table.name+"`.`" + this.typeKey + "`");
      }
      if (this.cnumKey != null)
      {
        caze.select("`"+this.table.name+"`.`" + this.cnumKey + "`");
      }
      else
      {
        caze.select("'1' AS `cnum`");
      }
      if (this.snumKey != null)
      {
        caze.select("`"+this.table.name+"`.`" + this.snumKey + "`");
      }
      else
      {
        caze.select("'0' AS `snum`");
      }
    }

    //** 查询列表 **/

    if (! rd.containsKey( Cnst.PN_KEY )
    &&  ! rd.containsKey( Cnst.GN_KEY )
    &&  ! rd.containsKey( Cnst.RN_KEY ))
    {
      rd.put(Cnst.RN_KEY, 0); // 默认不分页
    }
    Map  data = super.getList(rd, caze);
    List list = (List) data.get("list");

    //** 附带路径 **/

    if (list.isEmpty())
    {
        return data;
    }

    byte   pth = Synt.declare(rd.get("md"),(byte)  0 );
    String pid = Synt.declare(rd.get(this.pidKey), "");
    if (pid.length() == 0)
    {
        pid = this.rootId;
    }

    if (4 == (4 & pth))
    {
      //List path = this.getParentIds(pid);

      Iterator it = list.iterator();
      while (it.hasNext())
      {
        Map info = (Map)it.next();
        String id = (String)info.get(this.table.primaryKey);
        List subPath = new ArrayList( /* path */ );
        info.put("path", subPath);

        subPath.addAll(this.getParentIds(id, pid));
      }
    }
    else
    if (8 == (8 & pth))
    {
      //List path = this.getParents(pid);

      Iterator it = list.iterator();
      while (it.hasNext())
      {
        Map info = (Map)it.next();
        String id = (String)info.get(this.table.primaryKey);
        List subPath = new ArrayList( /* path */ );
        info.put("path", subPath);

        subPath.addAll(this.getParents  (id, pid));
      }
    }

    return data;
  }

  /**
   * 获取树
   *
   * @param rd
   * @return 树列表
   * @throws app.hongs.HongsException
   */
  @Override
  public Map getList(Map rd)
    throws HongsException
  {
    return this.getList(rd, null);
  }

  //** 标准模型方法 **/

  /**
   * 添加节点
   *
   * @param rd
   * @return 节点ID
   * @throws app.hongs.HongsException
   */
  @Override
  public String add(Map rd)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap( );
    }

    String pid = (String) rd.get(this.pidKey);

    // 默认加到根节点下
    if (pid == null || pid.length() == 0)
    {
      pid =  this.rootId ;
      rd.put(this.pidKey , pid);
    }

    // 默认添加到末尾
    if (!rd.containsKey(this.snumKey))
    {
      int num = this.getLastSerialNum(pid) +1;
      rd.put(this.snumKey, num);
    }

    // 默认没有子节点
    if (!rd.containsKey(this.cnumKey))
    {
      rd.put(this.cnumKey, "0");
    }

    String id = super.add(rd);

    // 将父节点的子节点数量加1
    this.chgChildsNum(pid, 1);

    return id;
  }

  /**
   * 更新节点
   *
   * @param id
   * @param rd
   * @return 节点ID
   * @throws app.hongs.HongsException
   */
  @Override
  public int put(String id, Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap( );
    }

    /**
     * 如有指定bid(BeforeID)
     * 则将新的pid(ParentID)重设为其pid
     */
    String bid = (String) rd.get(this.bidKey);
    if (bid != null && bid.length() != 0)
    {
      rd.put(this.pidKey, this.getParentId(bid));
    }

    String newPid = (String) rd.get(this.pidKey);
    String oldPid = this.getParentId (id);
    int    ordNum = this.getSerialNum(id);
    int    i = super.put( id, rd , caze );

    /**
     * 如果有指定新的pid且不同于旧的pid, 则
     * 将其新的父级子节点数目加1
     * 将其旧的父级子节点数目减1
     * 将其置于新父级列表的末尾
     * 并将旧的弟节点序号往前加1
     */
    if(null != newPid && !"".equals(newPid) && !oldPid.equals(newPid))
    {
      if (this.cnumKey != null)
      {
        this.chgChildsNum(newPid,  1);
        this.chgChildsNum(oldPid, -1);
      }

      if (this.snumKey != null)
      {
        this.setSerialNum(id, -1);
        this.chgSerialNum(oldPid, -1, ordNum, -1);
      }
    }

    /**
     * 如果有指定bid
     * 且其位置有所改变
     * 则将节点排序置为bid前
     */
    if (this.snumKey != null)
    {
      ordNum = this.getSerialNum(id);
      int ordNum2 = -1;

      if (null != bid && !"".equals(bid))
      {
        ordNum2 = this.getSerialNum(bid);
        if (ordNum2 > ordNum)
        {
          ordNum2 -= 1;
        }
      }

      if (ordNum2 > -1 && ordNum2 != ordNum)
      {
        this.chgSerialNum(id, ordNum2 - ordNum);
      }
    }

    return i;
  }

  /**
   * 删除节点
   *
   * @param id
   * @param caze
   * @return 删除条数
   * @throws app.hongs.HongsException
   */
  @Override
  public int del(String id, FetchCase caze)
    throws HongsException
  {
    String pid = this.getParentId(id);
    int on = this.getSerialNum(id);

    int i = super.del(id, caze);

    // 父级节点子节点数目减1
    this.chgChildsNum(pid, -1);

    // 弟弟节点排序数目减1
    this.chgSerialNum(pid, -1, on, -1);

    // 删除全部子节点
    List ids = this.getChildIds(id);
    Iterator it = ids.iterator();
    while (it.hasNext()) {
      this.del((String)it.next());
    }

    return i;
  }

  @Override
  protected void filter(FetchCase caze, Map rd)
    throws HongsException
  {
    super.filter(caze, rd);

    if (!rd.containsKey(Cnst.OB_KEY))
    {
      if (this.snumKey != null)
      {
        caze.orderBy("`"
            + this.table.name
            +"`.`"
            + this.snumKey
            + "`");
      } else
      if (this.cnumKey != null)
      {
        caze.orderBy("(CASE WHEN `"
            + this.table.name
            + "`.`"
            + this.cnumKey
            + "` > 0 THEN 1 END) DESC");
      }
    }
  }

  //** 树基础操作 **/

  public String getParentId(String id)
    throws HongsException
  {
    String sql = "SELECT `"
            + this.pidKey +
            "` FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.table.primaryKey +
            "` = ?";
    Map info = this.db.fetchOne(sql, id);
    return (String)info.get(this.pidKey);
  }

  public Map getParent(String id)
    throws HongsException
  {
    String sql = "SELECT * FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.table.primaryKey +
            "` = ?";
    Map info = this.db.fetchOne(sql, id);
    return !info.isEmpty() ? info : null;
  }

  public List<String> getParentIds(String id, String rootId)
    throws HongsException
  {
    List<String> ids = new ArrayList();
    String pid = this.getParentId(id );
    if (pid != null)
    {
      ids.add( pid );
      if (!pid.equals(rootId))
      {
        ids.addAll(this.getParentIds(pid, rootId));
      }
    }
    return ids;
  }

  public List<Map> getParents(String id, String rootId)
    throws HongsException
  {
    List<Map> nds = new ArrayList();
    Map pnd  = this.getParent( id );
    if (pnd != null)
    {
      nds.add( pnd );
      String pid = (String) pnd.get(this.pidKey);
      if (!pid.equals(rootId))
      {
        nds.addAll(this.getParents(pid, rootId));
      }
    }
    return nds;
  }

  public List<String> getParentIds(String id)
    throws HongsException
  {
    return this.getParentIds(id, this.rootId);
  }

  public List<Map> getParents(String id)
    throws HongsException
  {
    return this.getParents(id, this.rootId);
  }

  public List<String> getChildIds(String id, boolean all)
    throws HongsException
  {
    String sql;
    if (this.cnumKey == null)
    {
      sql = "SELECT `"
            + this.table.primaryKey +
            "` FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.pidKey +
            "` = ?";
    }
    else
    {
      sql = "SELECT `"
            + this.cnumKey +
            "`, `"
            + this.table.primaryKey +
            "` FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.pidKey +
            "` = ?";
    }
    List list = this.db.fetchAll(sql,id);

    FetchMore join = new FetchMore(list);
    List      cids = new ArrayList(join.maping(this.table.primaryKey).keySet());

    if (all)
    {
      Iterator it = list.iterator();
      while (it.hasNext())
      {
        Map info = (Map)it.next();
        String cid = (String)info.get(this.table.primaryKey);

        int num;
        if (this.cnumKey == null)
        {
          num = 1; // 总是尝试获取就行了 //this.getChildsNum(cid);
        }
        else
        {
          num = Integer.parseInt((String)info.get(this.cnumKey));
        }

        if (num > 0)
        {
          cids.addAll(this.getChildIds(cid, all));
        }
      }
    }

    return cids;
  }

  public List<Map> getChilds(String id, boolean all)
    throws HongsException
  {
    String sql;
    sql = "SELECT * FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.pidKey +
            "` = ?";
    List list = this.db.fetchAll(sql,id);

    if (all)
    {
      Iterator it = list.iterator();
      while (it.hasNext())
      {
        Map info = (Map)it.next();
        String cid = (String)info.get(this.table.primaryKey);

        int num;
        if (this.cnumKey == null)
        {
          num = 1; // 总是尝试获取就行了 //this.getChildsNum(cid);
        }
        else
        {
          num = Integer.parseInt((String)info.get(this.cnumKey));
        }

        if (num > 0)
        {
          list.addAll(this.getChildIds(cid, all));
        }
      }
    }

    return list;
  }

  public List<String> getChildIds(String id)
    throws HongsException
  {
    return this.getChildIds(id, false);
  }

  public List<Map> getChilds(String id)
    throws HongsException
  {
    return this.getChilds(id, false);
  }

  //** 子数目相关 **/

  public int getChildsNum(String id)
    throws HongsException
  {
    if (this.cnumKey == null)
    {
      return this.getRealChildsNum(id, null);
    }

    String sql = "SELECT `"
            + this.cnumKey +
            "` FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.table.primaryKey +
            "` = ?";
    List params = new ArrayList();
    params.add(id);

    Map info = this.db.fetchOne(sql, params.toArray());
    if (info.isEmpty())
    {
      return 0;
    }

    Object cn = info.get(this.cnumKey);
    return Integer.parseInt(cn.toString());
  }

  public int getRealChildsNum(String id, Collection excludeIds)
    throws HongsException
  {
    String sql = "SELECT COUNT(`"
            + this.table.primaryKey +
            "`) AS __count__ FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.pidKey +
            "` = ?";
    List params = new ArrayList();
    params.add(id);

    if (excludeIds != null)
    {
      sql += " `" + this.table.primaryKey + "` NOT IN (?)";
      params.add(excludeIds);
    }

    Map info = this.db.fetchOne(sql, params.toArray());
    if (info.isEmpty())
    {
      return 0;
    }

    Object cn = info.get("__count__");
    return Integer.parseInt(cn.toString());
  }

  public int getRealChildsNum(String id)
    throws HongsException
  {
    return this.getRealChildsNum(id, null);
  }

  public void setChildsNum(String id, int num)
    throws HongsException
  {
    if (this.cnumKey == null)
    {
      return;
    }

    if (num < 0)
    {
      num = this.getRealChildsNum(id) + Math.abs(num);
    }

    String sql = "UPDATE `"
            + this.table.tableName +
            "` SET `"
            + this.cnumKey +
            "` = ?" +
            " WHERE `"
            + this.table.primaryKey
            + "` = ?";
    List params = new ArrayList();
    params.add(num);
    params.add(id);
    this.db.execute(sql, params);
  }

  public void chgChildsNum(String id, int off)
    throws HongsException
  {
    if (this.cnumKey == null || off == 0)
    {
      return;
    }

    String sql = "UPDATE `"
            + this.table.tableName +
            "` SET `"
            + this.cnumKey +
            "` = `"
            + this.cnumKey +
            "` "
            + (off > 0 ? "+ "+off : "- "+Math.abs(off)) +
            " WHERE `"
            + this.table.primaryKey +
            "` = ?";
    List params = new ArrayList();
    params.add(id);
    this.db.execute(sql, params);
  }

  //** 排序号相关 **/

  public int getSerialNum(String id)
    throws HongsException
  {
    if (this.snumKey == null)
    {
      return 0;
    }

    String sql = "SELECT `"
            + this.snumKey +
            "` FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.table.primaryKey +
            "` = ?";

    Map info = this.db.fetchOne(sql, id);
    if (info.isEmpty())
    {
      return 0;
    }

    Object on = info.get(this.snumKey);
    return Integer.parseInt(on.toString());
  }

  public int getLastSerialNum(String pid, Collection excludeIds)
    throws HongsException
  {
    if (this.snumKey == null)
    {
      return this.getRealChildsNum(pid, excludeIds);
    }

    String sql = "SELECT `"
            + this.snumKey +
            "` FROM `"
            + this.table.tableName +
            "` WHERE `"
            + this.pidKey +
            "` = ?";
    List params = new ArrayList();
    params.add(pid);

    if (excludeIds != null)
    {
      sql += " AND `" + this.table.primaryKey + "` NOT IN (?)";
      params.add(excludeIds);
    }

    sql += " ORDER BY `" + this.snumKey + "` DESC";

    Map info = this.db.fetchOne(sql, params.toArray());
    if (info.isEmpty())
    {
      return 0;
    }

    Object on = info.get(this.snumKey);
    return Integer.parseInt(on.toString());
  }

  public int getLastSerialNum(String pid)
    throws HongsException
  {
    return this.getLastSerialNum(pid, null);
  }

  public void setSerialNum(String id, int num)
    throws HongsException
  {
    if (this.snumKey == null)
    {
      return;
    }

    if (num < 0)
    {
      String pid = this.getParentId(id);
      Set ids = new HashSet();
      ids.add(id);
      num = this.getLastSerialNum(pid, ids) + Math.abs(num);
    }

    String sql = "UPDATE `"
            + this.table.tableName +
            "` SET `"
            + this.snumKey +
            "` = ?" +
            " WHERE `"
            + this.table.primaryKey +
            "` = ?";
    List params = new ArrayList();
    params.add(num);
    params.add(id);
    this.db.execute(sql, params);
  }

  public void chgSerialNum(String id, int off)
    throws HongsException
  {
    if (this.snumKey == null || off == 0)
    {
      return;
    }

    String pid = this.getParentId(id);
    int oldNum = this.getSerialNum(id);
    int newNum = oldNum + off;
    if (off < 0)
    {
      this.chgSerialNum(pid, +1, newNum, oldNum - 1);
    }
    else
    {
      this.chgSerialNum(pid, -1, oldNum + 1, newNum);
    }

    String sql = "UPDATE `"
            + this.table.tableName +
            "` SET `"
            + this.snumKey +
            "` = `"
            + this.snumKey +
            "` "
            + (off > 0 ? "+ "+off : "- "+Math.abs(off)) +
            " WHERE `"
            + this.table.primaryKey +
            "` = ?";
    List params = new ArrayList();
    params.add(id);
    this.db.execute(sql, params);
  }

  public void chgSerialNum(String pid, int off, int pos1, int pos2)
    throws HongsException
  {
    if (this.snumKey == null || off == 0 || (pos1 < 0 && pos2 < 0))
    {
      return;
    }

    if (pos1 > pos2 && pos1 >= 0 && pos2 >= 0)
    {
      int pos3 = pos1;
      pos1 = pos2;
      pos2 = pos3;
    }

    String sqlAnd = "";
    if (pos1 > -1)
    {
      sqlAnd += " AND `"+this.snumKey+"` >= "+pos1;
    }
    if (pos2 > -1)
    {
      sqlAnd += " AND `"+this.snumKey+"` <= "+pos2;
    }

    String sql = "UPDATE `"
            + this.table.tableName +
            "` SET `"
            + this.snumKey +
            "` = `"
            + this.snumKey +
            "` "
            + (off > 0 ? "+ "+off : "- "+Math.abs(off)) +
            " WHERE `"
            + this.pidKey +
            "` = ?"
            + sqlAnd;
    List params = new ArrayList();
    params.add(pid);
    this.db.execute(sql, params);
  }

  //** 检查及修复 **/

  public void checkAndRepair(String pid)
    throws HongsException
  {
    String sql;
    String cid;
    int    num = 0;

    if (this.cnumKey != null)
    {
      sql = "SELECT COUNT(`"+this.table.primaryKey  +"`) AS _count_"+
            " FROM  `"      +this.table.tableName   +"`"+
            " WHERE `"      +this.pidKey            +"` = '"+pid+"'"+
            " GROUP BY `"   +this.pidKey            +"`";
      Map row = this.db.fetchOne(sql);
      if (!row.isEmpty())
      {
        num = Synt.declare(row.get("_count_") , num);
        sql = "UPDATE `"    +this.table.tableName   +"`"+
              "  SET  `"    +this.cnumKey           +"` = '"+num+"'"+
              " WHERE `"    +this.table.primaryKey  +"` = '"+pid+"'";
        this.db.execute(sql);
      }
    }

    if (this.snumKey != null)
    {
      sql = "SELECT `"      +this.table.primaryKey  +"`"+
            " FROM  `"      +this.table.tableName   +"`"+
            " WHERE `"      +this.pidKey            +"` = '"+pid+"'"+
            " ORDER BY `"   +this.snumKey           +"`";
      List rows = this.db.fetchAll(sql);
      if (!rows.isEmpty())
      {
        Iterator it = rows.iterator();
        while (it.hasNext())
        {
          Map row = (Map)it.next();
          cid = row.get(this.table.primaryKey).toString();
          sql = "UPDATE `"  +this.table.tableName   +"`"+
                "  SET  `"  +this.snumKey           +"` = '"+num+"'"+
                " WHERE `"  +this.table.primaryKey  +"` = '"+cid+"'";
          this.db.execute(/**/sql);
          this.checkAndRepair(cid);
          num ++;
        }
      }
    }
    else
    {
      List cids = this.getChildIds(pid);
      if (!cids.isEmpty())
      {
        Iterator it = cids.iterator();
        while (it.hasNext())
        {
          cid = (String) it.next();
          this.checkAndRepair(cid);
        }
      }
    }
  }

  public void checkAndRepair()
    throws HongsException
  {
    this.checkAndRepair(this.rootId);
  }

    /**
     * 此方法用于获取和构建树形模型唯一对象
     *
     * @param model
     * @return
     * @throws HongsException
     */
    public static Mtree getInstance(Model model) throws HongsException {
        Map    core  = model.db.modelObjects;
        String name  = model.table.name;
        Object minst = core.get( name );
        Mtree  mtree ;

        if (minst != null && minst instanceof Mview) {
            return ( Mtree ) minst ;
        }

        name  = name +":Mtree";
        minst = core.get(name);

        if (minst != null && minst instanceof Mview) {
            return ( Mtree ) minst ;
        }

        /**
         * 不在开始检查 model 的类型,
         * 是为了总是将 mtree 放入到模型库中管理,
         * 可以避免当前 model 关联的模型再关联回来时重复构造.
         */
        if (model instanceof Mtree) {
            mtree  = ( Mtree ) model ;
            core.put (name , mtree);
        } else {
            mtree  = new Mtree(model);
            core.put (name , mtree);
        }

        return mtree;
    }

}
