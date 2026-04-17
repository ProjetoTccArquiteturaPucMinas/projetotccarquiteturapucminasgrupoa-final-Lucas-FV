package com.example.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;
import com.example.marketplace.model.CategoriaProduto;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

 public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {
    List<ItemCarrinho> itens = new ArrayList<>();

    // 1. Monta os itens do carrinho e busca no repositório
    for (SelecaoCarrinho selecao : selecoes) {
        Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

        itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
    }

    // 2. Calcula o subtotal (Soma de Preço * Quantidade)
    BigDecimal subtotal = itens.stream()
            .map(ItemCarrinho::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 3. Calcula Desconto por Quantidade Total de Itens
    int totalItens = itens.stream().mapToInt(ItemCarrinho::getQuantidade).sum();
    BigDecimal percentualQuantidade = calcularPercentualPorQuantidade(totalItens);

    // 4. Calcula Desconto Adicional por Categoria (Acumulado por unidade)
    BigDecimal percentualCategorias = itens.stream()
            .map(item -> calcularPercentualCategoria(item.getProduto().getCategoria())
                         .multiply(BigDecimal.valueOf(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 5. Soma os descontos e aplica a Regra de Desconto Máximo (25%)
    BigDecimal percentualDescontoTotal = percentualQuantidade.add(percentualCategorias);
    BigDecimal limiteMaximo = new BigDecimal("25.00");

    if (percentualDescontoTotal.compareTo(limiteMaximo) > 0) {
        percentualDescontoTotal = limiteMaximo;
    }

    // 6. Cálculos Finais
    // valorDesconto = (subtotal * percentual) / 100
    BigDecimal valorDesconto = subtotal.multiply(percentualDescontoTotal)
            .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
    
    BigDecimal total = subtotal.subtract(valorDesconto);

    return new ResumoCarrinho(itens, subtotal, percentualDescontoTotal, valorDesconto, total);
}

// --- Métodos Auxiliares para Organização ---

private BigDecimal calcularPercentualPorQuantidade(int quantidade) {
    if (quantidade >= 4) return new BigDecimal("10");
    if (quantidade == 3) return new BigDecimal("7");
    if (quantidade == 2) return new BigDecimal("5");
    return BigDecimal.ZERO;
}

private BigDecimal calcularPercentualCategoria(CategoriaProduto categoria) {
    return switch (categoria) {
        case CAPINHA, FONE -> new BigDecimal("3");
        case CARREGADOR -> new BigDecimal("5");
        case PELICULA, SUPORTE -> new BigDecimal("2");
        default -> BigDecimal.ZERO;
    };
}
}
