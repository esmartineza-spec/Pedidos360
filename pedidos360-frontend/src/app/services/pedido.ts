export interface Pedido {
  id: number;
  clienteId: string;
  items: PedidoItem[];
  estado: string;
}

export interface PedidoItem {
  id?: number;
  productId: number;
  cantidad: number;
}

export interface CrearPedido {
  clienteId: string;
  items: Array<Pick<PedidoItem, "productId" | "cantidad">>;
}

export interface FormularioPedido {
  clienteId: string;
  productoId: number;
  cantidad: number;
}
