package io.netty.example.equipment;

public class NettyServerConfig {

    public boolean NettyPooledByteBufAllocatorEnable = false;
    public int SocketSndbufSize = 65535;
    public int SocketRcvbufSize = 65535;
    private int serverWorkerThreads = 128;
    private int serverCallbackExecutorThreads = 0;
    private int serverSelectorThreads = Runtime.getRuntime().availableProcessors() * 2;
    private int serverOnewaySemaphoreValue = 32;
    private int serverAsyncSemaphoreValue = 64;
    private int serverChannelMaxIdleTimeSeconds = 120;

    public boolean isNettyPooledByteBufAllocatorEnable() {
        return NettyPooledByteBufAllocatorEnable;
    }

    public void setNettyPooledByteBufAllocatorEnable(boolean nettyPooledByteBufAllocatorEnable) {
        NettyPooledByteBufAllocatorEnable = nettyPooledByteBufAllocatorEnable;
    }

    public int getSocketSndbufSize() {
        return SocketSndbufSize;
    }

    public void setSocketSndbufSize(int socketSndbufSize) {
        SocketSndbufSize = socketSndbufSize;
    }

    public int getSocketRcvbufSize() {
        return SocketRcvbufSize;
    }

    public void setSocketRcvbufSize(int socketRcvbufSize) {
        SocketRcvbufSize = socketRcvbufSize;
    }

    public int getServerWorkerThreads() {
        return serverWorkerThreads;
    }

    public void setServerWorkerThreads(int serverWorkerThreads) {
        this.serverWorkerThreads = serverWorkerThreads;
    }

    public int getServerCallbackExecutorThreads() {
        return serverCallbackExecutorThreads;
    }

    public void setServerCallbackExecutorThreads(int serverCallbackExecutorThreads) {
        this.serverCallbackExecutorThreads = serverCallbackExecutorThreads;
    }

    public int getServerSelectorThreads() {
        return serverSelectorThreads;
    }

    public void setServerSelectorThreads(int serverSelectorThreads) {
        this.serverSelectorThreads = serverSelectorThreads;
    }

    public int getServerOnewaySemaphoreValue() {
        return serverOnewaySemaphoreValue;
    }

    public void setServerOnewaySemaphoreValue(int serverOnewaySemaphoreValue) {
        this.serverOnewaySemaphoreValue = serverOnewaySemaphoreValue;
    }

    public int getServerAsyncSemaphoreValue() {
        return serverAsyncSemaphoreValue;
    }

    public void setServerAsyncSemaphoreValue(int serverAsyncSemaphoreValue) {
        this.serverAsyncSemaphoreValue = serverAsyncSemaphoreValue;
    }

    public int getServerChannelMaxIdleTimeSeconds() {
        return serverChannelMaxIdleTimeSeconds;
    }

    public void setServerChannelMaxIdleTimeSeconds(int serverChannelMaxIdleTimeSeconds) {
        this.serverChannelMaxIdleTimeSeconds = serverChannelMaxIdleTimeSeconds;
    }

}
