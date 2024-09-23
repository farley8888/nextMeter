package com.serial.opt;

/**
 * 循环队列缓存数据
 * Created by fanjc on 2016/7/5 005.
 */

public class CircleBuff
{
    private static final JLog LOG = new JLog("CircleBuff", false, JLog.TYPE_DEBUG);
    /**缓冲区*/
    private byte[] mBuffData;
    private int mReadIndex;
    private int mWriteIndex;
    private int mCapacity;
    private boolean isSameRound;
    /**同步锁*/
    private java.util.concurrent.locks.Lock mLock;
    private java.util.concurrent.locks.Condition mReadCondition;
    private java.util.concurrent.locks.Condition mWriteCondition;

    public CircleBuff(int capacity)
    {
        this.mCapacity = capacity;
        this.mBuffData = new byte[this.mCapacity];
        this.isSameRound = true;
        this.mLock = new java.util.concurrent.locks.ReentrantLock();
        this.mReadCondition = this.mLock.newCondition();
        this.mWriteCondition = this.mLock.newCondition();
    }

    public void release()
    {
        this.mBuffData = null;
        this.mLock = null;
        this.mReadCondition = null;
        this.mWriteCondition = null;
    }

    private boolean writeData(byte b)
    {
        this.mLock.lock();
        boolean result = false;
        try
        {
            if (((this.isSameRound) && (this.mWriteIndex >= this.mReadIndex)) || ((!this.isSameRound) && (this.mWriteIndex < this.mReadIndex)))
            {
                this.mBuffData[(this.mWriteIndex++)] = b;
                if (this.mWriteIndex >= this.mCapacity)
                {
                    this.isSameRound = (!this.isSameRound);
                    this.mWriteIndex = 0;
                }
                result = true;
            }
            this.mReadCondition.signal();
        }
        finally
        {
            this.mLock.unlock();
        }

        return result;
    }

    public void writeDataWithBlock(byte b)
    {
        this.mLock.lock();
        try
        {
            while (!writeData(b))
            {
                try
                {
                    LOG.print("write await");
                    this.mWriteCondition.await();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        finally
        {
            this.mLock.unlock();
        }
    }

    public void writeDataWithBlock(byte[] data, int offset, int len)
    {
        for (int i = offset; i < len; i++)
        {
            writeDataWithBlock(data[i]);
        }
    }

    private byte readData()
    {
        this.mLock.lock();
        byte b = this.mBuffData[(this.mReadIndex++)];
        try
        {
            if (this.mReadIndex >= this.mCapacity)
            {
                this.isSameRound = (!this.isSameRound);
                this.mReadIndex = 0;
            }
            this.mWriteCondition.signal();
        }
        finally
        {
            this.mLock.unlock();
        }

        return b;
    }

    private boolean isCanReadData()
    {
        return ((this.isSameRound) && (this.mWriteIndex > this.mReadIndex)) || ((!this.isSameRound) && (this.mWriteIndex <= this.mReadIndex));
    }

    public byte readDataWithBlock()
    {
        this.mLock.lock();
        try
        {
            while (!isCanReadData())
            {
                try
                {
                    LOG.print("read await");
                    this.mReadCondition.await();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        finally
        {
            this.mLock.unlock();
        }
        return readData();
    }

    public int readDataWithBlock(byte[] data, int offset, int len)
    {
        int size = 0;
        for (int i = offset; i < len; i++)
        {
            if (isCanReadData())
            {
                data[i] = readData();
                size++;
            }
        }
        return size;
    }

    public int getCount()
    {
        if (this.isSameRound)
        {
            return this.mWriteIndex - this.mReadIndex;
        }

        return this.mReadIndex + this.mWriteIndex;
    }

    public void clear()
    {
        this.mReadIndex = (this.mWriteIndex = 0);
        this.isSameRound = true;
    }
}
