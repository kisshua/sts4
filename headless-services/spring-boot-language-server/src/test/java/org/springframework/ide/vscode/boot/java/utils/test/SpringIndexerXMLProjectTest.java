/*******************************************************************************
 * Copyright (c) 2019 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.utils.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.ide.vscode.boot.bootiful.BootLanguageServerTest;
import org.springframework.ide.vscode.boot.bootiful.SymbolProviderTestConf;
import org.springframework.ide.vscode.boot.java.utils.SpringSymbolIndex;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.project.harness.BootLanguageServerHarness;
import org.springframework.ide.vscode.project.harness.ProjectsHarness;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Martin Lippert
 */
@RunWith(SpringRunner.class)
@BootLanguageServerTest
@Import(SymbolProviderTestConf.class)
public class SpringIndexerXMLProjectTest {

	@Autowired private BootLanguageServerHarness harness;
	@Autowired private SpringSymbolIndex indexer;
	@Autowired private JavaProjectFinder projectFinder;

	private File directory;
	private String projectDir;
	private IJavaProject project;

	@Before
	public void setup() throws Exception {
		harness.intialize(null);
		indexer.configureIndexer(true);

		directory = new File(ProjectsHarness.class.getResource("/test-projects/test-annotation-indexing-xml-project/").toURI());
		projectDir = directory.toURI().toString();

		// trigger project creation
		project = projectFinder.find(new TextDocumentIdentifier(projectDir)).get();

		CompletableFuture<Void> initProject = indexer.waitOperation();
		initProject.get(5, TimeUnit.SECONDS);
	}

	@Test
	public void testScanningSimpleSpringXMLConfig() throws Exception {
		List<? extends SymbolInformation> allSymbols = indexer.getAllSymbols("");

		assertEquals(4, allSymbols.size());

		String docUri = directory.toPath().resolve("config/simple-spring-config.xml").toUri().toString();
		assertTrue(containsSymbol(allSymbols, "@+ 'transactionManager' DataSourceTransactionManager", docUri, 6, 14, 6, 37));
		assertTrue(containsSymbol(allSymbols, "@+ 'jdbcTemplate' JdbcTemplate", docUri, 8, 14, 8, 31));
		assertTrue(containsSymbol(allSymbols, "@+ 'namedParameterJdbcTemplate' NamedParameterJdbcTemplate", docUri, 12, 14, 12, 45));
		assertTrue(containsSymbol(allSymbols, "@+ 'persistenceExceptionTranslationPostProcessor' PersistenceExceptionTranslationPostProcessor", docUri, 18, 10, 18, 97));
	}

	private boolean containsSymbol(List<? extends SymbolInformation> symbols, String name, String uri, int startLine, int startCHaracter, int endLine, int endCharacter) {
		for (Iterator<? extends SymbolInformation> iterator = symbols.iterator(); iterator.hasNext();) {
			SymbolInformation symbol = iterator.next();

			if (symbol.getName().equals(name)
					&& symbol.getLocation().getUri().equals(uri)
					&& symbol.getLocation().getRange().getStart().getLine() == startLine
					&& symbol.getLocation().getRange().getStart().getCharacter() == startCHaracter
					&& symbol.getLocation().getRange().getEnd().getLine() == endLine
					&& symbol.getLocation().getRange().getEnd().getCharacter() == endCharacter) {
				return true;
			}
 		}

		return false;
	}

}
