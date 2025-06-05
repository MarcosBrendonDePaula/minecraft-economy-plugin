package com.minecraft.economy.lottery;

import java.util.UUID;

/**
 * Representa um bilhete de loteria
 */
public class LotteryTicket {

    private final UUID playerId;
    private final int ticketNumber;
    private final long purchaseTime;
    private final String drawType;

    /**
     * Cria um novo bilhete de loteria
     * @param playerId UUID do jogador
     * @param ticketNumber Número do bilhete
     * @param purchaseTime Timestamp da compra
     * @param drawType Tipo de sorteio (daily, weekly, etc)
     */
    public LotteryTicket(UUID playerId, int ticketNumber, long purchaseTime, String drawType) {
        this.playerId = playerId;
        this.ticketNumber = ticketNumber;
        this.purchaseTime = purchaseTime;
        this.drawType = drawType;
    }

    /**
     * Obtém o UUID do jogador
     * @return UUID do jogador
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Obtém o número do bilhete
     * @return Número do bilhete
     */
    public int getTicketNumber() {
        return ticketNumber;
    }

    /**
     * Obtém o timestamp da compra
     * @return Timestamp da compra
     */
    public long getPurchaseTime() {
        return purchaseTime;
    }

    /**
     * Obtém o tipo de sorteio
     * @return Tipo de sorteio
     */
    public String getDrawType() {
        return drawType;
    }
}
