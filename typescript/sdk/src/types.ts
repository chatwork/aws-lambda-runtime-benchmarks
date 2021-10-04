export interface MyItem {
    pk: string
    sk: string
}

export interface QueryResults {
    count: Array<{ count: number }>
    items: Array<MyItem>
}
