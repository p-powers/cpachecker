// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

lexer grammar CTALexer;

MODULE
	: 'MODULE'
	;

ROOT
	: 'ROOT'
	;

LOCAL
	: 'LOCAL'
	;

INPUT
	: 'INPUT'
	;

OUTPUT
	: 'OUTPUT'
	;

MULTREST
	: 'MULTREST'
	;

SYNC
	: 'SYNC'
	;

ANALOG
	: 'ANALOG'
	;

STOPWATCH
	: 'STOPWATCH'
	;

CLOCK
	: 'CLOCK'
	;

DISCRETE
	: 'DISCRETE'
	;

CONST
	: 'CONST'
	;

INST
	: 'INST'
	;

FROM
	: 'FROM'
	;

WITH
	: 'WITH'
	;

AS
	: 'AS'
	;

INITIAL
	: 'INITIAL'
	;

AND
	: 'AND'
	;

STATE
	: 'STATE'
	;

EQUAL
	: '='
	;

LESS
	: '<'
	;

LESSEQUAL
	: '<='
	;

GREATER
	: '>'
	;

GREATEREQUAL
	: '>='
	;

AUTOMATON
	: 'AUTOMATON'
	;

INV
	: 'INV'
	;

DERIV
	: 'DERIV'
	;

DER
	: 'DER'
	;

TRANS
	: 'TRANS'
	;

GOTO
	: 'GOTO'
	;

GUARD
	: 'GUARD'
	;

RESET
	: 'RESET'
	;

TARGET
	: 'TARGET'
	;

QUESTIONMARK
	: '?'
	;

EXCLAMATIONMARK
	: '!'
	;

HASH
	: '#'
	;

IDENTIFIER
  	:  ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*
  	;

fragment
NONZERODIGIT
	: [1-9]
	;

fragment
DIGIT
	: [0-9]
	;

NUMBER
	: '0' 
	| '-'? NONZERODIGIT DIGIT*
	;

SEMICOLON
	: ';'
	;

COLON
	: ':'
	;

LBRACKET
	: '{'
	;

RBRACKET
	: '}'
	;

LPAREN
	: '('
	;

RPAREN
	: ')'
	;

COMMA
	: ','
	;

WHITESPACE
    :   [ \t\u000C\r\n]+
        -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> skip
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> skip
    ;