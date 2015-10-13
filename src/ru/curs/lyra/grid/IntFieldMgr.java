package ru.curs.lyra.grid;

import java.math.BigInteger;

/**
 * Менеджер целочисленного поля, входящего в первичный ключ.
 */
public final class IntFieldMgr extends KeyManager {

	private long min;
	private long max;
	private BigInteger card;
	private int value;

	public IntFieldMgr() {
		setBounds(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public IntFieldMgr(int min, int max) {
		setBounds(min, max);
	}

	/**
	 * Установка минимального и максимального значения поля.
	 * 
	 * @param min
	 *            минимальное значение.
	 * @param max
	 *            максимальное значение.
	 */
	public void setBounds(int min, int max) {
		// We should have min < max, or else arythmetic errors will occur
		// min == max is no good either, since max-min == 0 division.
		if (max <= min)
			throw new IllegalArgumentException();
		this.min = min;
		this.max = max;
		this.card = BigInteger.valueOf(this.max - this.min + 1);
	}

	@Override
	public BigInteger cardinality() {
		return card;
	}

	@Override
	public BigInteger getOrderValue() {
		return BigInteger.valueOf(value - min);
	}

	/**
	 * Текущее значение поля.
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Установка значения поля.
	 * 
	 * @param value
	 *            Значение (целое число).
	 */
	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public double getPosition() {
		return (double) (value - min) / (double) (max - min);
	}

	@Override
	public void setOrderValue(BigInteger value) {
		this.value = value.add(BigInteger.valueOf(min)).intValue();
	}
}