package com.github.otymko.phoenixbsl.logic.lsp;

import com.github.otymko.phoenixbsl.logic.PhoenixAPI;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionKindCapabilities;
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class BSLBinding {
  private static final String TRACE_LEVEL = "verbose";
  private final BSLLanguageClient client;
  private LanguageServer server;
  private final InputStream in;
  private final OutputStream out;
  private final Thread thread = new Thread(this::start);

  public BSLBinding(BSLLanguageClient client, InputStream in, OutputStream out) {
    this.client = client;
    this.in = in;
    this.out = out;
  }

  public void startInThread() {
    LOGGER.info("Подключение к серверу LSP");
    thread.setDaemon(true);
    thread.setName("BSLLanguageLauncher");
    thread.start();
  }

  @VisibleForTesting
  private void start() {
    var launcher = LSPLauncher.createClientLauncher(client, in, out);
    var future = launcher.startListening();

    server = launcher.getRemoteProxy();

    while (true) {
      try {
        future.get();
        return;
      } catch (InterruptedException e) {
        LOGGER.error(e.getMessage(), e);
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

  }

  public CompletableFuture<InitializeResult> initialize() {
    var params = new InitializeParams();
    params.setProcessId(PhoenixAPI.getProcessId());
    params.setTrace(TRACE_LEVEL);
    var serverCapabilities = new ClientCapabilities();

    var textDocument = new TextDocumentClientCapabilities();
    var codeActionCapabilities = new CodeActionCapabilities(true);
    textDocument.setCodeAction(codeActionCapabilities);
    textDocument
      .setCodeAction(
        new CodeActionCapabilities(
          new CodeActionLiteralSupportCapabilities(
            new CodeActionKindCapabilities(Arrays.asList("", CodeActionKind.QuickFix))),
          false));
    serverCapabilities.setTextDocument(textDocument);
    params.setCapabilities(serverCapabilities);

    return server.initialize(params);
  }

  public void textDocumentDidOpen(URI uri, String text) {
    var params = new DidOpenTextDocumentParams();
    var item = new TextDocumentItem();
    item.setLanguageId("bsl");
    item.setUri(uri.toString());
    item.setText(text);
    params.setTextDocument(item);
    server.getTextDocumentService().didOpen(params);
  }

  public void textDocumentDidChange(URI uri, String text) {
    var params = new DidChangeTextDocumentParams();
    var versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier();
    versionedTextDocumentIdentifier.setUri(uri.toString());
    versionedTextDocumentIdentifier.setVersion(0);
    params.setTextDocument(versionedTextDocumentIdentifier);
    var textDocument = new TextDocumentContentChangeEvent();
    textDocument.setText(text);
    List<TextDocumentContentChangeEvent> list = new ArrayList<>();
    list.add(textDocument);
    params.setContentChanges(list);
    server.getTextDocumentService().didChange(params);
  }

  public void textDocumentDidSave(URI uri) {
    var paramsSave = new DidSaveTextDocumentParams();
    var textDocumentIdentifier = new TextDocumentIdentifier();
    textDocumentIdentifier.setUri(uri.toString());
    paramsSave.setTextDocument(textDocumentIdentifier);
    server.getTextDocumentService().didSave(paramsSave);
  }

  public List<Either<Command, CodeAction>> textDocumentCodeAction(URI uri, List<Diagnostic> listDiagnostic,
                                                                  List<String> only) throws ExecutionException, InterruptedException {

    var params = new CodeActionParams();

    var textDocumentIdentifier = new TextDocumentIdentifier();
    textDocumentIdentifier.setUri(uri.toString());

    var context = new CodeActionContext();
    context.setDiagnostics(listDiagnostic);
    if (!only.isEmpty()) {
      context.setOnly(only);
    }

    params.setRange(new Range());
    params.setTextDocument(textDocumentIdentifier);
    params.setContext(context);
    return server.getTextDocumentService().codeAction(params).get();
  }

  public CompletableFuture<List<? extends TextEdit>> textDocumentFormatting(URI uri) {
    var paramsFormatting = new DocumentFormattingParams();
    var identifier = new TextDocumentIdentifier();
    identifier.setUri(uri.toString());
    paramsFormatting.setTextDocument(identifier);
    var options = new FormattingOptions(4, false);
    paramsFormatting.setOptions(options);
    return server.getTextDocumentService().formatting(paramsFormatting);
  }

  public CompletableFuture<Object> shutdown() {
    return server.shutdown();
  }

  public void exit() {
    server.exit();
  }

}
