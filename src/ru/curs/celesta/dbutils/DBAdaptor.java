package ru.curs.celesta.dbutils;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import ru.curs.celesta.AppSettings;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.ConnectionPool;
import ru.curs.celesta.score.BinaryColumn;
import ru.curs.celesta.score.BooleanColumn;
import ru.curs.celesta.score.Column;
import ru.curs.celesta.score.DateTimeColumn;
import ru.curs.celesta.score.FKRule;
import ru.curs.celesta.score.FloatingColumn;
import ru.curs.celesta.score.ForeignKey;
import ru.curs.celesta.score.Grain;
import ru.curs.celesta.score.Index;
import ru.curs.celesta.score.IntegerColumn;
import ru.curs.celesta.score.StringColumn;
import ru.curs.celesta.score.Table;

/**
 * Адаптер соединения с БД, выполняющий команды, необходимые системе обновления.
 * 
 */
public abstract class DBAdaptor {

	/*
	 * NB для программистов. Класс большой, во избежание хаоса здесь порядок
	 * такой: прежде всего -- метод getAdaptor(), далее идут public final
	 * методы, далее --- внутренняя кухня (default final и default static
	 * методы), в самом низу -- все объявления абстрактных методов.
	 */
	static final String NOT_IMPLEMENTED_YET = "not implemented yet";

	static final Class<?>[] COLUMN_CLASSES = { IntegerColumn.class,
			StringColumn.class, BooleanColumn.class, FloatingColumn.class,
			BinaryColumn.class, DateTimeColumn.class };
	static final String COLUMN_NAME = "COLUMN_NAME";
	static final String ALTER_TABLE = "alter table ";

	/**
	 * Фабрика классов адаптеров подходящего под текущие настройки типа.
	 * 
	 * @throws CelestaException
	 *             При ошибке создания адаптера (например, при создании адаптера
	 *             не поддерживаемого типа).
	 */
	public static DBAdaptor getAdaptor() throws CelestaException {
		switch (AppSettings.getDBType()) {
		case MSSQL:
			return new MSSQLAdaptor();
		case MYSQL:
			return new MySQLAdaptor();
		case ORACLE:
			return new OraAdaptor();
		case POSTGRES:
			return new PostgresAdaptor();
		case UNKNOWN:
		default:
			throw new CelestaException("Unknown or unsupported database type.");
		}
	}

	/**
	 * Проверка на валидность соединения.
	 * 
	 * @param conn
	 *            соединение.
	 * @param timeout
	 *            тайм-аут.
	 * @return true если соединение валидно, иначе false
	 * @throws CelestaException
	 *             при возникновении ошибки работы с БД.
	 */
	public boolean isValidConnection(Connection conn, int timeout)
			throws CelestaException {
		try {
			return conn.isValid(timeout);
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
	}

	/**
	 * Получить шаблон имени таблицы.
	 */
	public String tableTemplate() {
		return "\"%s\".\"%s\"";
	}

	/**
	 * Удалить таблицу.
	 * 
	 * @param conn
	 *            Соединение с БД
	 * @param t
	 *            удаляемая таблица
	 * @throws CelestaException
	 *             в случае ошибки работы с БД
	 */
	public final void dropTable(Connection conn, Table t)
			throws CelestaException {
		try {
			String sql = String.format("DROP TABLE " + tableTemplate(), t
					.getGrain().getName(), t.getName());
			Statement stmt = conn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				stmt.close();
			}
			dropAutoIncrement(conn, t);
			conn.commit();
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
	}

	/**
	 * Возвращает true в том и только том случае, если база данных содержит
	 * пользовательские таблицы (т. е. не является пустой базой данных).
	 * 
	 * @throws CelestaException
	 *             ошибка БД
	 */
	public final boolean userTablesExist() throws CelestaException {
		Connection conn = ConnectionPool.get();
		try {
			return userTablesExist(conn);
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		} finally {
			ConnectionPool.putBack(conn);
		}
	}

	/**
	 * Создаёт в базе данных схему с указанным именем, если таковая схема ранее
	 * не существовала.
	 * 
	 * @param name
	 *            имя схемы.
	 * @throws CelestaException
	 *             только в том случае, если возник критический сбой при
	 *             создании схемы. Не выбрасывается в случае, если схема с
	 *             данным именем уже существует в базе данных.
	 */
	public final void createSchemaIfNotExists(String name)
			throws CelestaException {
		Connection conn = ConnectionPool.get();
		try {
			createSchemaIfNotExists(conn, name);
		} catch (SQLException e) {
			throw new CelestaException("Cannot create schema. "
					+ e.getMessage());
		} finally {
			ConnectionPool.putBack(conn);
		}
	}

	/**
	 * Создаёт в базе данных таблицу "с нуля".
	 * 
	 * @param conn
	 *            Соединение.
	 * @param table
	 *            Таблица для создания.
	 * @throws CelestaException
	 *             В случае возникновения критического сбоя при создании
	 *             таблицы, в том числе в случае, если такая таблица существует.
	 */
	public final void createTable(Connection conn, Table table)
			throws CelestaException {
		String def = tableDef(table);
		try {
			// System.out.println(def); // for debug purposes
			Statement stmt = conn.createStatement();
			try {
				stmt.executeUpdate(def);
			} finally {
				stmt.close();
			}
			manageAutoIncrement(conn, table);
			ConnectionPool.commit(conn);
		} catch (SQLException e) {
			throw new CelestaException("creating %s: %s", table.getName(),
					e.getMessage());
		}
	}

	/**
	 * Добавляет к таблице новую колонку.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * 
	 * @param c
	 *            Колонка для добавления.
	 * @throws CelestaException
	 *             при ошибке добавления колонки.
	 */
	public final void createColumn(Connection conn, Column c)
			throws CelestaException {
		String sql = String.format(ALTER_TABLE + tableTemplate() + " add %s", c
				.getParentTable().getGrain().getName(), c.getParentTable()
				.getName(), columnDef(c));
		try {
			Statement stmt = conn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				stmt.close();
			}
			manageAutoIncrement(conn, c.getParentTable());
		} catch (SQLException e) {
			throw new CelestaException("creating %s.%s: %s", c.getParentTable()
					.getName(), c.getName(), e.getMessage());
		}
	}

	/**
	 * Возвращает набор имён индексов, связанных с таблицами, лежащими в
	 * указанной грануле.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * @param g
	 *            Гранула, по таблицам которой следует просматривать индексы.
	 * @throws CelestaException
	 *             В случае сбоя связи с БД.
	 */
	public Map<DBIndexInfo, TreeMap<Short, String>> getIndices(Connection conn,
			Grain g) throws CelestaException {
		Map<DBIndexInfo, TreeMap<Short, String>> result = new HashMap<>();
		try {
			for (Table t : g.getTables().values()) {
				DatabaseMetaData metaData = conn.getMetaData();
				ResultSet rs = metaData.getIndexInfo(null, t.getGrain()
						.getName(), t.getName(), false, false);
				try {
					while (rs.next()) {
						String indName = rs.getString("INDEX_NAME");
						if (indName != null && rs.getBoolean("NON_UNIQUE")) {
							DBIndexInfo info = new DBIndexInfo(t.getName(),
									indName);
							TreeMap<Short, String> columns = result.get(info);
							if (columns == null) {
								columns = new TreeMap<>();
								result.put(info, columns);
							}
							columns.put(rs.getShort("ORDINAL_POSITION"),
									rs.getString(COLUMN_NAME));
						}
					}
				} finally {
					rs.close();
				}
			}
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
		return result;
	}

	/**
	 * Возвращает набор имён столбцов определённой таблицы.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * @param t
	 *            Таблица, по которой просматривать столбцы.
	 * 
	 * @throws CelestaException
	 *             в случае сбоя связи с БД.
	 */
	public Set<String> getColumns(Connection conn, Table t)
			throws CelestaException {
		Set<String> result = new LinkedHashSet<>();
		try {
			DatabaseMetaData metaData = conn.getMetaData();
			ResultSet rs = metaData.getColumns(null, t.getGrain().getName(),
					t.getName(), null);
			try {
				while (rs.next()) {
					String rColumnName = rs.getString(COLUMN_NAME);
					result.add(rColumnName);
				}
			} finally {
				rs.close();
			}
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
		return result;
	}

	/**
	 * Возвращает условие where на таблице, исходя из текущих фильтров.
	 * 
	 * @param filters
	 *            фильтры
	 * @throws CelestaException
	 *             в случае некорректного фильтра
	 */
	final String getWhereClause(Table t, Map<String, AbstractFilter> filters)
			throws CelestaException {
		if (filters == null)
			throw new IllegalArgumentException();
		StringBuilder whereClause = new StringBuilder();
		for (Entry<String, AbstractFilter> e : filters.entrySet()) {
			if (whereClause.length() > 0)
				whereClause.append(" and ");
			if (e.getValue() instanceof SingleValue)
				whereClause.append(String.format("(\"%s\" = ?)", e.getKey()));
			else if (e.getValue() instanceof Range)
				whereClause.append(String.format("(\"%s\" between ? and ?)",
						e.getKey()));
			else if (e.getValue() instanceof Filter) {
				Column c = t.getColumns().get(e.getKey());
				whereClause.append(((Filter) e.getValue()).makeWhereClause(c));
			}
		}
		return whereClause.toString();
	}

	/**
	 * Устанавливает параметры на запрос по фильтрам.
	 * 
	 * @param filters
	 *            Фильтры, с которыми вызывался getWhereClause
	 * @throws CelestaException
	 *             в случае сбоя JDBC
	 */
	final void fillSetQueryParameters(Map<String, AbstractFilter> filters,
			PreparedStatement result) throws CelestaException {
		int i = 1;
		for (AbstractFilter f : filters.values()) {
			if (f instanceof SingleValue) {
				setParam(result, i++, ((SingleValue) f).getValue());
			} else if (f instanceof Range) {
				setParam(result, i++, ((Range) f).getValueFrom());
				setParam(result, i++, ((Range) f).getValueTo());
			}
			// Пока что фильтры параметров не требуют
			// else if (f instanceof Filter)
			// throw new RuntimeException(NOT_IMPLEMENTED_YET);
		}
	}

	/**
	 * Создаёт в грануле индекс на таблице.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * 
	 * @param index
	 *            описание индекса.
	 * @throws CelestaException
	 *             Если что-то пошло не так.
	 */
	public final void createIndex(Connection conn, Index index)
			throws CelestaException {
		String sql = getCreateIndexSQL(index);
		try {
			Statement stmt = conn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				stmt.close();
			}
			ConnectionPool.commit(conn);
		} catch (SQLException e) {
			throw new CelestaException("Cannot create index '%s': %s",
					index.getName(), e.getMessage());
		}
	}

	/**
	 * Создаёт первичный ключ.
	 * 
	 * @param conn
	 *            соединение с БД.
	 * @param fk
	 *            первичный ключ
	 * @throws CelestaException
	 *             в случае неудачи создания ключа
	 */
	public final void createFK(Connection conn, ForeignKey fk)
			throws CelestaException {
		LinkedList<StringBuilder> sqlQueue = new LinkedList<>();

		// Строим запрос на создание FK
		StringBuilder sql = new StringBuilder();
		sql.append(ALTER_TABLE);
		sql.append(String.format(tableTemplate(), fk.getParentTable()
				.getGrain().getName(), fk.getParentTable().getName()));
		sql.append(" add constraint \"");
		sql.append(fk.getConstraintName());
		sql.append("\" foreign key (");
		boolean needComma = false;
		for (String name : fk.getColumns().keySet()) {
			if (needComma)
				sql.append(", ");
			sql.append('"');
			sql.append(name);
			sql.append('"');
			needComma = true;
		}
		sql.append(") references ");
		sql.append(String.format(tableTemplate(), fk.getReferencedTable()
				.getGrain().getName(), fk.getReferencedTable().getName()));
		sql.append("(");
		needComma = false;
		for (String name : fk.getReferencedTable().getPrimaryKey().keySet()) {
			if (needComma)
				sql.append(", ");
			sql.append('"');
			sql.append(name);
			sql.append('"');
			needComma = true;
		}
		sql.append(")");

		switch (fk.getDeleteRule()) {
		case SET_NULL:
			sql.append(" on delete set null");
			break;
		case CASCADE:
			sql.append(" on delete cascade");
			break;
		case NO_ACTION:
		default:
			break;
		}

		sqlQueue.add(sql);
		processCreateUpdateRule(fk, sqlQueue);

		// Построили, выполняем
		for (StringBuilder sqlStmt : sqlQueue) {
			String sqlstmt = sqlStmt.toString();

			// System.out.println("----------------");
			// System.out.println(sqlStmt);

			try {
				Statement stmt = conn.createStatement();
				try {
					stmt.executeUpdate(sqlstmt);
				} finally {
					stmt.close();
				}
			} catch (SQLException e) {
				if (!sqlstmt.startsWith("drop"))
					throw new CelestaException(
							"Cannot create foreign key '%s': %s",
							fk.getConstraintName(), e.getMessage());
			}
		}
	}

	/**
	 * Удаляет первичный ключ из базы данных.
	 * 
	 * @param conn
	 *            Соединение с БД
	 * @param grainName
	 *            имя гранулы
	 * @param tableName
	 *            Имя таблицы, на которой определён первичный ключ.
	 * @param fkName
	 *            Имя первичного ключа.
	 * @throws CelestaException
	 *             В случае сбоя в базе данных.
	 */
	public final void dropFK(Connection conn, String grainName,
			String tableName, String fkName) throws CelestaException {
		LinkedList<String> sqlQueue = new LinkedList<>();
		String sql = String.format("alter table " + tableTemplate()
				+ " drop constraint \"%s\"", grainName, tableName, fkName);
		sqlQueue.add(sql);
		processDropUpdateRule(sqlQueue, fkName);
		// Построили, выполняем
		for (String sqlStmt : sqlQueue) {
			// System.out.println(sqlStmt);
			try {
				Statement stmt = conn.createStatement();
				try {
					stmt.executeUpdate(sqlStmt);
				} finally {
					stmt.close();
				}
			} catch (SQLException e) {
				if (!sqlStmt.startsWith("drop trigger"))
					throw new CelestaException(
							"Cannot drop foreign key '%s': %s", fkName,
							e.getMessage());
			}
		}
	}

	void processDropUpdateRule(LinkedList<String> sqlQueue, String fkName) {

	}

	void processCreateUpdateRule(ForeignKey fk, LinkedList<StringBuilder> queue) {
		StringBuilder sql = queue.peek();
		switch (fk.getUpdateRule()) {
		case SET_NULL:
			sql.append(" on update set null");
			break;
		case CASCADE:
			sql.append(" on update cascade");
			break;
		case NO_ACTION:
		default:
			break;
		}
	}

	/**
	 * Удаляет в грануле индекс на таблице.
	 * 
	 * @param g
	 *            Гранула
	 * @param dBIndexInfo
	 *            Массив из двух элементов: имя таблицы, имя индекса
	 * @throws CelestaException
	 *             Если что-то пошло не так.
	 */
	public final void dropIndex(Grain g, DBIndexInfo dBIndexInfo)
			throws CelestaException {
		String sql = getDropIndexSQL(g, dBIndexInfo);
		Connection conn = ConnectionPool.get();
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			throw new CelestaException("Cannot drop index '%s': %s ",
					dBIndexInfo.getIndexName(), e.getMessage());
		} finally {
			ConnectionPool.putBack(conn);
		}
	}

	/**
	 * Возвращает PreparedStatement, содержащий отфильтрованный набор записей.
	 * 
	 * @param conn
	 *            Соединение.
	 * @param t
	 *            Таблица.
	 * @param filters
	 *            Фильтры на таблице.
	 * @param orderBy
	 *            Порядок сортировки.
	 * @throws CelestaException
	 *             Ошибка БД или некорректный фильтр.
	 */
	public final PreparedStatement getRecordSetStatement(Connection conn,
			Table t, Map<String, AbstractFilter> filters, List<String> orderBy)
			throws CelestaException {

		// Готовим условие where
		String whereClause = getWhereClause(t, filters);

		// Соединяем полученные компоненты в стандартный запрос
		// SELECT..FROM..WHERE..ORDER BY
		String sql = getSelectFromOrderBy(t, whereClause, orderBy);

		try {
			PreparedStatement result = conn.prepareStatement(sql);
			// А теперь заполняем параметры
			fillSetQueryParameters(filters, result);
			return result;
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
	}

	/**
	 * Возвращает наименование типа столбца, соответствующее базе данных.
	 * 
	 * @param c
	 *            Колонка в score
	 */
	final String dbFieldType(Column c) {
		return getColumnDefiner(c).dbFieldType();
	}

	final String columnDef(Column c) {
		return getColumnDefiner(c).getFullDefinition(c);
	}

	final String tableDef(Table table) {
		StringBuilder sb = new StringBuilder();
		// Определение таблицы с колонками
		sb.append(String.format("create table " + tableTemplate() + "(\n",
				table.getGrain().getName(), table.getName()));
		boolean multiple = false;
		for (Column c : table.getColumns().values()) {
			if (multiple)
				sb.append(",\n");
			sb.append("  " + columnDef(c));
			multiple = true;
		}
		sb.append(",\n");
		// Определение первичного ключа (он у нас всегда присутствует)
		sb.append(String.format("  constraint \"%s\" primary key (",
				table.getPkConstraintName()));
		multiple = false;
		for (String s : table.getPrimaryKey().keySet()) {
			if (multiple)
				sb.append(", ");
			sb.append('"');
			sb.append(s);
			sb.append('"');
			multiple = true;
		}
		sb.append(")\n)");
		return sb.toString();
	}

	static PreparedStatement prepareStatement(Connection conn, String sql)
			throws CelestaException {
		try {
			return conn.prepareStatement(sql);
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
	}

	static String getFieldList(Iterable<String> fields) {
		// NB: этот метод возможно нужно будет сделать виртуальным, чтобы учесть
		// особенности синтаксиса разных баз данных
		StringBuilder sb = new StringBuilder();
		for (String c : fields) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append('"');
			sb.append(c);
			sb.append('"');
		}
		return sb.toString();
	}

	static String getTableFieldsListExceptBLOBs(Table t) {
		List<String> flds = new LinkedList<>();
		for (Map.Entry<String, Column> e : t.getColumns().entrySet()) {
			if (!(e.getValue() instanceof BinaryColumn))
				flds.add(e.getKey());
		}
		return getFieldList(flds);
	}

	static void runUpdateColumnSQL(Connection conn, Column c, String sql)
			throws CelestaException {
		// System.out.println(sql); //for debug
		try {
			Statement stmt = conn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			throw new CelestaException(
					"Cannot modify column %s on table %s.%s: %s", c.getName(),
					c.getParentTable().getGrain().getName(), c.getParentTable()
							.getName(), e.getMessage());

		}
	}

	static FKRule getFKRule(String rule) {
		if ("NO ACTION".equalsIgnoreCase(rule)
				|| "RECTRICT".equalsIgnoreCase(rule))
			return FKRule.NO_ACTION;
		if ("SET NULL".equalsIgnoreCase(rule))
			return FKRule.SET_NULL;
		if ("CASCADE".equalsIgnoreCase(rule))
			return FKRule.CASCADE;
		return null;
	}

	final String getSelectFromOrderBy(Table t, String whereClause,
			List<String> orderBy) {
		String sqlfrom = String.format("select %s from " + tableTemplate(),
				getTableFieldsListExceptBLOBs(t), t.getGrain().getName(),
				t.getName());

		String sqlwhere = "".equals(whereClause) ? "" : " where " + whereClause;

		String orderByList = getFieldList(orderBy);
		String sqlorder = "".equals(orderByList) ? "" : " order by "
				+ orderByList;

		return sqlfrom + sqlwhere + sqlorder;
	}

	static String getRecordWhereClause(Table t) {
		StringBuilder whereClause = new StringBuilder();
		for (String fieldName : t.getPrimaryKey().keySet())
			whereClause.append(String.format("%s(\"%s\" = ?)",
					whereClause.length() > 0 ? " and " : "", fieldName));
		return whereClause.toString();
	}

	static void setParam(PreparedStatement stmt, int i, Object v)
			throws CelestaException {
		try {
			if (v == null)
				stmt.setNull(i, java.sql.Types.NULL);
			else if (v instanceof Integer)
				stmt.setInt(i, (Integer) v);
			else if (v instanceof Double)
				stmt.setDouble(i, (Double) v);
			else if (v instanceof String)
				stmt.setString(i, (String) v);
			else if (v instanceof Boolean)
				stmt.setBoolean(i, (Boolean) v);
			else if (v instanceof Date) {
				Timestamp d = new Timestamp(((Date) v).getTime());
				stmt.setTimestamp(i, d);
			} else if (v instanceof BLOB) {
				Blob b = stmt.getConnection().createBlob();
				((BLOB) v).saveToJDBCBlob(b);
				stmt.setBlob(i, b);
			}
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
	}

	static Set<String> sqlToStringSet(Connection conn, String sql)
			throws CelestaException {
		Set<String> result = new HashSet<String>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			try {
				while (rs.next()) {
					result.add(rs.getString(1));
				}
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			throw new CelestaException(e.getMessage());
		}
		return result;
	}

	final PreparedStatement getSetCountStatement(Connection conn, Table t,
			Map<String, AbstractFilter> filters) throws CelestaException {
		String whereClause = getWhereClause(t, filters);
		String sql = String.format("select count(*) from " + tableTemplate()
				+ ("".equals(whereClause) ? "" : " where " + whereClause), t
				.getGrain().getName(), t.getName());
		PreparedStatement result = prepareStatement(conn, sql);
		fillSetQueryParameters(filters, result);
		return result;
	}

	abstract ColumnDefiner getColumnDefiner(Column c);

	abstract boolean tableExists(Connection conn, String schema, String name)
			throws CelestaException;

	abstract boolean userTablesExist(Connection conn) throws SQLException;

	abstract void createSchemaIfNotExists(Connection conn, String name)
			throws SQLException;

	abstract void manageAutoIncrement(Connection conn, Table t)
			throws SQLException;

	abstract void dropAutoIncrement(Connection conn, Table t)
			throws SQLException;

	abstract PreparedStatement getOneRecordStatement(Connection conn, Table t)
			throws CelestaException;

	abstract PreparedStatement getOneFieldStatement(Connection conn, Column c)
			throws CelestaException;

	abstract PreparedStatement deleteRecordSetStatement(Connection conn,
			Table t, Map<String, AbstractFilter> filters)
			throws CelestaException;

	abstract PreparedStatement getInsertRecordStatement(Connection conn,
			Table t, boolean[] nullsMask) throws CelestaException;

	abstract int getCurrentIdent(Connection conn, Table t)
			throws CelestaException;

	abstract PreparedStatement getUpdateRecordStatement(Connection conn,
			Table t, boolean[] equalsMask) throws CelestaException;

	abstract PreparedStatement getDeleteRecordStatement(Connection conn, Table t)
			throws CelestaException;

	abstract String getCreateIndexSQL(Index index);

	abstract String getDropIndexSQL(Grain g, DBIndexInfo dBIndexInfo);

	/**
	 * Возвращает информацию о столбце.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * 
	 * @param c
	 *            Столбец.
	 * @throws CelestaException
	 *             в случае сбоя связи с БД.
	 */
	abstract DBColumnInfo getColumnInfo(Connection conn, Column c)
			throws CelestaException;

	/**
	 * Обновляет на таблице колонку.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * 
	 * @param c
	 *            Колонка для обновления.
	 * @throws CelestaException
	 *             при ошибке обновления колонки.
	 */
	abstract void updateColumn(Connection conn, Column c, DBColumnInfo actual)
			throws CelestaException;

	/**
	 * Возвращает информацию о первичном ключе таблицы.
	 * 
	 * @param conn
	 *            Соединение с БД.
	 * @param t
	 *            Таблица, информацию о первичном ключе которой необходимо
	 *            получить.
	 * @throws CelestaException
	 *             в случае сбоя связи с БД.
	 */
	abstract DBPKInfo getPKInfo(Connection conn, Table t)
			throws CelestaException;

	/**
	 * Удаляет первичный ключ на таблице с использованием известного имени
	 * первичного ключа.
	 * 
	 * @param conn
	 *            Соединение с базой данных.
	 * @param t
	 *            Таблица.
	 * @param pkName
	 *            Имя первичного ключа.
	 * @throws CelestaException
	 *             в случае сбоя связи с БД.
	 */
	abstract void dropPK(Connection conn, Table t, String pkName)
			throws CelestaException;

	/**
	 * Создаёт первичный ключ на таблице в соответствии с метаописанием.
	 * 
	 * @param conn
	 *            Соединение с базой данных.
	 * @param t
	 *            Таблица.
	 * @throws CelestaException
	 *             неудача создания первичного ключа (например, неуникальные
	 *             записи).
	 */
	abstract void createPK(Connection conn, Table t) throws CelestaException;

	abstract List<DBFKInfo> getFKInfo(Connection conn, Grain g)
			throws CelestaException;
}

/**
 * Класс, ответственный за генерацию определения столбца таблицы в разных СУБД.
 * 
 */
abstract class ColumnDefiner {
	static final String DEFAULT = "default ";

	abstract String dbFieldType();

	/**
	 * Возвращает определение колонки, содержащее имя, тип и NULL/NOT NULL (без
	 * DEFAULT). Требуется для механизма изменения колонок.
	 * 
	 * @param c
	 *            колонка.
	 */
	abstract String getMainDefinition(Column c);

	/**
	 * Отдельно возвращает DEFAULT-определение колонки.
	 * 
	 * @param c
	 *            колонка.
	 */
	abstract String getDefaultDefinition(Column c);

	/**
	 * Возвращает полное определение колонки (для создания колонки).
	 * 
	 * @param c
	 *            колонка
	 */
	String getFullDefinition(Column c) {
		return join(getMainDefinition(c), getDefaultDefinition(c));
	}

	String nullable(Column c) {
		return c.isNullable() ? "null" : "not null";
	}

	/**
	 * Соединяет строки через пробел.
	 * 
	 * @param ss
	 *            массив строк для соединения в виде свободного параметра.
	 */
	static String join(String... ss) {
		StringBuilder sb = new StringBuilder();
		boolean multiple = false;
		for (String s : ss)
			if (!"".equals(s)) {
				if (multiple)
					sb.append(' ' + s);
				else {
					sb.append(s);
					multiple = true;
				}
			}
		return sb.toString();
	}
}
