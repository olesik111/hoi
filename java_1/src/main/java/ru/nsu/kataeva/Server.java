package ru.nsu.kataeva;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class Server {
    private static final ConcurrentHashMap<String, CompletableFuture<byte[]>> cache = new ConcurrentHashMap<>();
    private static final Queue<WriteTask> writeQueue = new ConcurrentLinkedQueue<>();
    private static ExecutorService workerPool;
    private static PrivateKey caPrivateKey;
    private static String issuerName;

    static class WriteTask {
        SocketChannel channel;
        byte[] data;

        WriteTask(SocketChannel c, byte[] d) {
            channel = c; data = d;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java KeyGenServer <port> <threads> <issuer_name> <ca_key_file>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);
        issuerName = args[2];
        String caKeyPath = args[3];

        byte[] keyBytes = Files.readAllBytes(Paths.get(caKeyPath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        caPrivateKey = kf.generatePrivate(spec);

        workerPool = Executors.newFixedThreadPool(threads);

        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port), 150);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(256));

                } else if (key.isReadable()) {
                    readClientRequest(key, selector);

                } else if (key.isWritable()) {
                    writeToClient(key);
                }
            }

            WriteTask task;
            while ((task = writeQueue.poll()) != null) {
                SelectionKey key = task.channel.keyFor(selector);
                if (key != null && key.isValid()) {
                    key.attach(ByteBuffer.wrap(task.data));
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        }
    }

    private static void readClientRequest(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        int read = channel.read(buffer);

        if (read == -1) {
            channel.close();
            return;
        }

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                String name = new String(bytes, 0, i, StandardCharsets.US_ASCII);
                key.interestOps(0);

                cache.computeIfAbsent(name, n -> CompletableFuture.supplyAsync(() -> generatePayload(n), workerPool))
                        .thenAccept(data -> {
                            writeQueue.offer(new WriteTask(channel, data));
                            selector.wakeup();
                        });
                return;
            }
        }
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
    }

    private static void writeToClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        if (buffer != null) {
            channel.write(buffer);
            if (!buffer.hasRemaining()) {
                channel.close();
            }
        }
    }

    private static byte[] generatePayload(String subjectName) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            X500Name issuer = new X500Name("CN=" + issuerName);
            X500Name subject = new X500Name("CN=" + subjectName);

            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date();
            Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000);

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuer, serial, notBefore, notAfter, subject, keyPair.getPublic());

            byte[] certBytes = certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA")
                    .build(caPrivateKey)).getEncoded();

            byte[] keyBytes = keyPair.getPrivate().getEncoded();

            ByteBuffer out = ByteBuffer.allocate(8 + certBytes.length + keyBytes.length);
            out.putInt(certBytes.length).put(certBytes).putInt(keyBytes.length).put(keyBytes);

            return out.array();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}