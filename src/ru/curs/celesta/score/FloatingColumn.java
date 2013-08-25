package ru.curs.celesta.score;

/**
 * Колонка с типом REAL.
 * 
 */
public final class FloatingColumn extends Column {

	private Double defaultvalue;

	public FloatingColumn(Table table, String name) throws ParseException {
		super(table, name);
	}

	@Override
	protected void setDefault(String lexvalue) {
		defaultvalue = (lexvalue == null) ? null : Double.parseDouble(lexvalue);
	}

	/**
	 * Возвращает значение по умолчанию.
	 */
	public Double getDefaultvalue() {
		return defaultvalue;
	}

	@Override
	protected String getDefaultDefault() {
		return "0";
	}

	@Override
	public String pythonDefaultValue() {
		return "0";
	}

	@Override
	public String jdbcGetterName() {
		return "getDouble";
	}
}
