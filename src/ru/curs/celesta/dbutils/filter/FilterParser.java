/* Generated By:JavaCC: Do not edit this line. FilterParser.java */
package ru.curs.celesta.dbutils.filter;

import java.util.Date;
import java.io.StringReader;
import ru.curs.celesta.CelestaException;

public class FilterParser implements FilterParserConstants {
   private static final String INVALID_QUOTED_FORMAT = "Invalid quoted string format.";

   public static enum FilterType { NUMERIC , TEXT , DATETIME , OTHER };

   public interface SQLTranslator {
       String translateDate(String date) throws CelestaException;
   }

   private FilterType filterType;
   private String fieldName;
   private SQLTranslator tr;

   FilterParser(StringReader sr, SQLTranslator tr) {
        this(sr);
        this.tr = tr;
   }

   public static String translateFilter(FilterType fieldType, String fieldName, String filter, SQLTranslator tr) throws CelestaException {
         StringReader sr = new StringReader(filter);
     try {
                 FilterParser parser = new FilterParser(sr, tr);
                 try {
            return parser.filterExpr(fieldType, fieldName);
                 } catch (ParseException e) {
                    throw new CelestaException("Invalid field filter '%s': %s", filter, e.getMessage());
         } catch (TokenMgrError e) {
            throw new CelestaException("Invalid field filter '%s': %s", filter, e.getMessage());
         }
     } finally {
         sr.close();
     }
   }

   private String translateDate(String dateLiteral) throws ParseException {
        try{
            return tr.translateDate(dateLiteral);
        } catch (CelestaException e) {
            throw new ParseException(e.getMessage());
        }
   }


        public static String quoteString(String lexvalue) {
                StringBuilder sb = new StringBuilder();
                sb.append('\u005c'');
                for (int i = 0; i < lexvalue.length(); i++) {
                        char c = lexvalue.charAt(i);
                        sb.append(c);
                        if (c == '\u005c'')
                                sb.append('\u005c'');
                }
                sb.append('\u005c'');
                return sb.toString();
        }
        public static String unquoteString(String lexvalue) throws ParseException {
                StringBuilder sb = new StringBuilder();
                int state = 0;
                for (int i = 0; i < lexvalue.length(); i++) {
                        char c = lexvalue.charAt(i);
                        switch (state) {
                        case 0:
                                if (c == '\u005c'') {
                                        state = 1;
                                } else {
                                        throw new ParseException(INVALID_QUOTED_FORMAT);
                                }
                                break;
                        case 1:
                                if (c == '\u005c'') {
                                        state = 2;
                                } else {
                                        sb.append(c);
                                }
                                break;
                        case 2:
                                if (c == '\u005c'') {
                                        sb.append('\u005c'');
                                        state = 1;
                                } else {
                                        throw new ParseException(INVALID_QUOTED_FORMAT);
                                }
                        default:
                        }
                }
                return sb.toString();
        }

  final public String filterExpr(FilterType filterType, String fieldName) throws ParseException {
  this.filterType = filterType;
  this.fieldName = fieldName;
  String result;
    if (filterType == FilterType.OTHER) {
      result = nullExpr();
      jj_consume_token(0);
    } else if (filterType != FilterType.OTHER) {
      result = expr();
      jj_consume_token(0);
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return result;}
    throw new Error("Missing return statement in function");
  }

  final public String nullExpr() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case S_NULL:
      jj_consume_token(S_NULL);
                    {if (true) return String.format("%s is null", fieldName);}
      break;
    case 7:
      jj_consume_token(7);
      jj_consume_token(S_NULL);
                        {if (true) return String.format("not (%s is null)", fieldName);}
      break;
    default:
      jj_la1[0] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String expr() throws ParseException {
  String buf;
  StringBuilder result = new StringBuilder();
    buf = singleExpr();
                        result.append(buf);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 8:
    case 9:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 8:
        label_1:
        while (true) {
          jj_consume_token(8);
          buf = singleExpr();
                              result.append(" and " +  buf);
          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
          case 8:
            ;
            break;
          default:
            jj_la1[1] = jj_gen;
            break label_1;
          }
        }
        break;
      case 9:
        label_2:
        while (true) {
          jj_consume_token(9);
          buf = singleExpr();
                              result.append(" or " +buf);
          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
          case 9:
            ;
            break;
          default:
            jj_la1[2] = jj_gen;
            break label_2;
          }
        }
        break;
      default:
        jj_la1[3] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    default:
      jj_la1[4] = jj_gen;
      ;
    }
     {if (true) return result.toString();}
    throw new Error("Missing return statement in function");
  }

  final public String singleExpr() throws ParseException {
  String buf;
  String result;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 10:
      jj_consume_token(10);
      buf = expr();
      jj_consume_token(11);
                                 result = "(" + buf + ")";
      break;
    case 7:
      jj_consume_token(7);
      buf = singleExpr();
                                 result = "not (" + buf + ")";
      break;
    default:
      jj_la1[5] = jj_gen;
      if (jj_2_1(1)) {
        buf = term();
                                 result =  buf;
      } else {
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    {if (true) return result;}
    throw new Error("Missing return statement in function");
  }

  final public String term() throws ParseException {
 Token tok;
 String result;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case S_NULL:
      tok = jj_consume_token(S_NULL);
                                                result = String.format("%s is null", fieldName);
      break;
    default:
      jj_la1[6] = jj_gen;
      if (filterType == FilterType.NUMERIC) {
        result = numTerm();
      } else if (filterType == FilterType.DATETIME) {
        result = dateTerm();
      } else if (filterType == FilterType.TEXT) {
        result = textTerm();
      } else {
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
   {if (true) return result;}
    throw new Error("Missing return statement in function");
  }

  final public String numTerm() throws ParseException {
                   String val1; String val2;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 12:
      jj_consume_token(12);
      val1 = number();
                            {if (true) return String.format("%s > %s", fieldName, val1);}
      break;
    case 13:
      jj_consume_token(13);
      val1 = number();
                            {if (true) return String.format("%s < %s", fieldName, val1);}
      break;
    case 14:
      jj_consume_token(14);
      val1 = number();
                            {if (true) return String.format("%s <= %s", fieldName, val1);}
      break;
    case S_DOUBLE:
    case S_INTEGER:
      val1 = number();
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 14:
        jj_consume_token(14);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case S_DOUBLE:
        case S_INTEGER:
          val2 = number();
                                              {if (true) return String.format("%s between %s and %s", fieldName, val1, val2);}
          break;
        default:
          jj_la1[7] = jj_gen;
          ;
        }
                                                                                                                      {if (true) return String.format("%s >= %s", fieldName, val1);}
        break;
      default:
        jj_la1[8] = jj_gen;
        ;
      }
    {if (true) return String.format("%s = %s", fieldName, val1);}
      break;
    default:
      jj_la1[9] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String number() throws ParseException {
                  Token tok;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case S_INTEGER:
      tok = jj_consume_token(S_INTEGER);
                        {if (true) return tok.toString();}
      break;
    case S_DOUBLE:
      tok = jj_consume_token(S_DOUBLE);
                        {if (true) return tok.toString();}
      break;
    default:
      jj_la1[10] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String dateTerm() throws ParseException {
                    String val1; String val2;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 12:
      jj_consume_token(12);
      val1 = date();
                          {if (true) return String.format("%s > %s", fieldName, val1);}
      break;
    case 13:
      jj_consume_token(13);
      val1 = date();
                          {if (true) return String.format("%s < %s", fieldName, val1);}
      break;
    case 14:
      jj_consume_token(14);
      val1 = date();
                          {if (true) return String.format("%s <= %s", fieldName, val1);}
      break;
    case S_CHAR_LITERAL:
      val1 = date();
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 14:
        jj_consume_token(14);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case S_CHAR_LITERAL:
          val2 = date();
                                          {if (true) return String.format("%s between %s and %s", fieldName, val1, val2);}
          break;
        default:
          jj_la1[11] = jj_gen;
          ;
        }
                                                                                                                  {if (true) return String.format("%s >= %s", fieldName, val1);}
        break;
      default:
        jj_la1[12] = jj_gen;
        ;
      }
    {if (true) return String.format("%s = %s", fieldName, val1);}
      break;
    default:
      jj_la1[13] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String date() throws ParseException {
                Token tok;
    tok = jj_consume_token(S_CHAR_LITERAL);
                             {if (true) return translateDate(tok.toString());}
    throw new Error("Missing return statement in function");
  }

  final public String textTerm() throws ParseException {
                    String val1; String val2; boolean ci = false; String fn = fieldName;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 15:
      jj_consume_token(15);
         ci = true; fn = "UPPER(" + fn + ")";
      break;
    default:
      jj_la1[14] = jj_gen;
      ;
    }
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 12:
      jj_consume_token(12);
      val1 = text(ci);
                            {if (true) return String.format("%s > %s", fn, val1);}
      break;
    case 13:
      jj_consume_token(13);
      val1 = text(ci);
                            {if (true) return String.format("%s < %s", fn, val1);}
      break;
    case 14:
      jj_consume_token(14);
      val1 = text(ci);
                            {if (true) return String.format("%s <= %s", fn, val1);}
      break;
    case 16:
      val1 = likeFilter(ci);
                            {if (true) return String.format("%s like %s", fn,  quoteString(val1));}
      break;
    case S_CHAR_LITERAL:
      val1 = text(ci);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 14:
      case 16:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case 14:
          jj_consume_token(14);
          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
          case S_CHAR_LITERAL:
            val2 = text(ci);
                                   {if (true) return String.format("%s between %s and %s", fn, val1, val2);}
            break;
          default:
            jj_la1[15] = jj_gen;
            ;
          }
                                                                                                    {if (true) return String.format("%s >= %s", fn, val1);}
          break;
        case 16:
          val2 = likeFilter(ci);
                                  {if (true) return String.format("%s like %s", fn,  quoteString(unquoteString(val1) + val2));}
          break;
        default:
          jj_la1[16] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        break;
      default:
        jj_la1[17] = jj_gen;
        ;
      }
         {if (true) return String.format("%s = %s", fn, val1);}
      break;
    default:
      jj_la1[18] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String likeFilter(boolean ci) throws ParseException {
StringBuilder sb = new StringBuilder();
String val;
    jj_consume_token(16);
         sb.append('%');
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case S_CHAR_LITERAL:
      val = text(ci);
                      sb.append (unquoteString(val));
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 16:
        val = likeFilter(ci);
                             sb.append (val);
        break;
      default:
        jj_la1[19] = jj_gen;
        ;
      }
      break;
    default:
      jj_la1[20] = jj_gen;
      ;
    }
      {if (true) return sb.toString();}
    throw new Error("Missing return statement in function");
  }

  final public String text(boolean ci) throws ParseException {
                          Token tok;
    tok = jj_consume_token(S_CHAR_LITERAL);
                             {if (true) return ci? tok.toString().toUpperCase() : tok.toString();}
    throw new Error("Missing return statement in function");
  }

  private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_3R_4() {
    if (jj_scan_token(S_NULL)) return true;
    return false;
  }

  private boolean jj_3R_3() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_4()) {
    jj_scanpos = xsp;
    jj_lookingAhead = true;
    jj_semLA = filterType == FilterType.NUMERIC;
    jj_lookingAhead = false;
    if (!jj_semLA || jj_3R_5()) {
    jj_scanpos = xsp;
    jj_lookingAhead = true;
    jj_semLA = filterType == FilterType.DATETIME;
    jj_lookingAhead = false;
    if (!jj_semLA || jj_3R_6()) {
    jj_scanpos = xsp;
    jj_lookingAhead = true;
    jj_semLA = filterType == FilterType.TEXT;
    jj_lookingAhead = false;
    if (!jj_semLA || jj_3R_7()) return true;
    }
    }
    }
    return false;
  }

  private boolean jj_3R_24() {
    if (jj_3R_28()) return true;
    return false;
  }

  private boolean jj_3R_23() {
    if (jj_3R_27()) return true;
    return false;
  }

  private boolean jj_3R_22() {
    if (jj_scan_token(14)) return true;
    return false;
  }

  private boolean jj_3R_21() {
    if (jj_scan_token(13)) return true;
    return false;
  }

  private boolean jj_3R_20() {
    if (jj_scan_token(12)) return true;
    return false;
  }

  private boolean jj_3R_19() {
    if (jj_scan_token(15)) return true;
    return false;
  }

  private boolean jj_3_1() {
    if (jj_3R_3()) return true;
    return false;
  }

  private boolean jj_3R_10() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_19()) jj_scanpos = xsp;
    xsp = jj_scanpos;
    if (jj_3R_20()) {
    jj_scanpos = xsp;
    if (jj_3R_21()) {
    jj_scanpos = xsp;
    if (jj_3R_22()) {
    jj_scanpos = xsp;
    if (jj_3R_23()) {
    jj_scanpos = xsp;
    if (jj_3R_24()) return true;
    }
    }
    }
    }
    return false;
  }

  private boolean jj_3R_26() {
    if (jj_scan_token(S_CHAR_LITERAL)) return true;
    return false;
  }

  private boolean jj_3R_18() {
    if (jj_3R_26()) return true;
    return false;
  }

  private boolean jj_3R_17() {
    if (jj_scan_token(14)) return true;
    return false;
  }

  private boolean jj_3R_16() {
    if (jj_scan_token(13)) return true;
    return false;
  }

  private boolean jj_3R_9() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_15()) {
    jj_scanpos = xsp;
    if (jj_3R_16()) {
    jj_scanpos = xsp;
    if (jj_3R_17()) {
    jj_scanpos = xsp;
    if (jj_3R_18()) return true;
    }
    }
    }
    return false;
  }

  private boolean jj_3R_15() {
    if (jj_scan_token(12)) return true;
    return false;
  }

  private boolean jj_3R_30() {
    if (jj_scan_token(S_DOUBLE)) return true;
    return false;
  }

  private boolean jj_3R_29() {
    if (jj_scan_token(S_INTEGER)) return true;
    return false;
  }

  private boolean jj_3R_28() {
    if (jj_scan_token(S_CHAR_LITERAL)) return true;
    return false;
  }

  private boolean jj_3R_25() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_29()) {
    jj_scanpos = xsp;
    if (jj_3R_30()) return true;
    }
    return false;
  }

  private boolean jj_3R_14() {
    if (jj_3R_25()) return true;
    return false;
  }

  private boolean jj_3R_13() {
    if (jj_scan_token(14)) return true;
    return false;
  }

  private boolean jj_3R_12() {
    if (jj_scan_token(13)) return true;
    return false;
  }

  private boolean jj_3R_8() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_11()) {
    jj_scanpos = xsp;
    if (jj_3R_12()) {
    jj_scanpos = xsp;
    if (jj_3R_13()) {
    jj_scanpos = xsp;
    if (jj_3R_14()) return true;
    }
    }
    }
    return false;
  }

  private boolean jj_3R_11() {
    if (jj_scan_token(12)) return true;
    return false;
  }

  private boolean jj_3R_27() {
    if (jj_scan_token(16)) return true;
    return false;
  }

  private boolean jj_3R_7() {
    if (jj_3R_10()) return true;
    return false;
  }

  private boolean jj_3R_6() {
    if (jj_3R_9()) return true;
    return false;
  }

  private boolean jj_3R_5() {
    if (jj_3R_8()) return true;
    return false;
  }

  /** Generated Token Manager. */
  public FilterParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  /** Whether we are looking ahead. */
  private boolean jj_lookingAhead = false;
  private boolean jj_semLA;
  private int jj_gen;
  final private int[] jj_la1 = new int[21];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0xc0,0x100,0x200,0x300,0x300,0x480,0x40,0xc,0x4000,0x700c,0xc,0x20,0x4000,0x7020,0x8000,0x20,0x14000,0x14000,0x17020,0x10000,0x20,};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[1];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public FilterParser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public FilterParser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new FilterParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public FilterParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new FilterParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public FilterParser(FilterParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(FilterParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = jj_lookingAhead ? jj_scanpos : token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[17];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 21; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 17; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 1; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
